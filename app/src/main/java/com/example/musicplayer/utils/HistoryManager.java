package com.example.musicplayer.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.musicplayer.api.HistoryRecordRequest;
import com.example.musicplayer.api.PlayTrackResponse;
import com.example.musicplayer.api.SpotifyApi;

import java.util.HashSet;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Quản lý logic lưu lịch sử bài hát
 * - Kiểm tra settings bật/tắt
 * - Tránh lưu trùng lặp
 * - Kiểm tra thời gian phát tối thiểu
 * - Queue để retry khi offline
 */
public class HistoryManager {
    private static final String TAG = "HistoryManager";
    private static final String PREF_NAME = "HistorySettings";
    private static final String KEY_ENABLED = "history_enabled";
    private static final String KEY_MIN_DURATION = "min_play_duration_seconds";
    private static final String KEY_DEDUPE_WINDOW = "dedupe_window_seconds";
    private static final String KEY_RECENT_TRACKS = "recent_track_ids";

    // Default settings
    private static final boolean DEFAULT_ENABLED = true;
    private static final int DEFAULT_MIN_DURATION = 15; // 15 seconds minimum
    private static final int DEFAULT_DEDUPE_WINDOW = 300; // 5 minutes

    private final Context context;
    private final SharedPreferences prefs;
    private final Set<String> recentTrackIds;
    private long lastCleanupTime = 0;

    public HistoryManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.recentTrackIds = new HashSet<>(prefs.getStringSet(KEY_RECENT_TRACKS, new HashSet<>()));
    }

    /**
     * Kiểm tra xem có nên lưu lịch sử cho bài này không
     */
    public boolean shouldSaveHistory(String trackId, int playDurationSeconds) {
        // 1. Kiểm tra setting có bật không
        if (!isHistoryEnabled()) {
            Log.d(TAG, "⏸️ History tracking is disabled in settings");
            return false;
        }

        // 2. Kiểm tra track ID hợp lệ
        if (trackId == null || trackId.trim().isEmpty()) {
            Log.w(TAG, "⚠️ Invalid track ID, cannot save history");
            return false;
        }

        // 3. Kiểm tra thời gian phát tối thiểu
        int minDuration = getMinPlayDuration();
        if (playDurationSeconds < minDuration) {
            Log.d(TAG, "⏭️ Play duration (" + playDurationSeconds + "s) < minimum (" + minDuration
                    + "s), skipping history");
            return false;
        }

        // 4. Kiểm tra duplicate trong khoảng thời gian gần đây
        if (isRecentlyPlayed(trackId)) {
            Log.d(TAG, "🔁 Track was recently played (within dedupe window), skipping to avoid duplicate");
            return false;
        }

        Log.d(TAG, "✅ All checks passed, will save to history");
        return true;
    }

    /**
     * ⭐ RECOMMENDED: Lưu lịch sử bằng auto-save API
     * Endpoint này TỰ ĐỘNG lấy info từ Spotify và lưu vào history
     */
    public void saveHistoryAutoSave(
            SpotifyApi spotifyApi,
            String trackId,
            int playDurationSeconds,
            HistorySaveCallback callback) {

        Log.d(TAG, "🔍 saveHistoryAutoSave called for track: " + trackId + ", duration: " + playDurationSeconds + "s");

        if (!shouldSaveHistory(trackId, playDurationSeconds)) {
            Log.w(TAG, "❌ shouldSaveHistory returned false, skipping save");
            if (callback != null) {
                callback.onSkipped("History tracking disabled or conditions not met");
            }
            return;
        }

        // Đánh dấu track này đã được phát gần đây
        markAsRecentlyPlayed(trackId);

        Log.d(TAG, "📤 Auto-saving history for track: " + trackId + " (played " + playDurationSeconds + "s)");
        Log.d(TAG, "🌐 Calling API endpoint: POST /api/spotify/tracks/" + trackId + "/play?play_duration_seconds="
                + playDurationSeconds);

        spotifyApi.playTrack(trackId, playDurationSeconds).enqueue(new Callback<PlayTrackResponse>() {
            @Override
            public void onResponse(@NonNull Call<PlayTrackResponse> call,
                    @NonNull Response<PlayTrackResponse> response) {
                Log.d(TAG, "📥 Auto-save API response: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    PlayTrackResponse.TrackData track = response.body().data;
                    Log.d(TAG, "✅ History auto-saved successfully: " + track.name);
                    if (callback != null) {
                        callback.onSuccess();
                    }
                } else {
                    Log.e(TAG, "❌ Failed to auto-save history. HTTP " + response.code());
                    if (callback != null) {
                        callback.onError(response.code(), "HTTP " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<PlayTrackResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "❌ Network error while auto-saving history: " + t.getMessage());
                if (callback != null) {
                    callback.onError(0, t.getMessage());
                }
            }
        });
    }

    /**
     * Lưu lịch sử với SpotifyApi (Manual method - fallback)
     */
    public void saveHistory(
            SpotifyApi spotifyApi,
            String trackId,
            String trackName,
            String artistName,
            String album,
            int durationMs,
            int playDurationSeconds,
            HistorySaveCallback callback) {
        if (!shouldSaveHistory(trackId, playDurationSeconds)) {
            if (callback != null) {
                callback.onSkipped("History tracking disabled or conditions not met");
            }
            return;
        }

        // Đánh dấu track này đã được phát gần đây
        markAsRecentlyPlayed(trackId);

        HistoryRecordRequest request = new HistoryRecordRequest(
                trackId, trackName, artistName, album, durationMs, playDurationSeconds);

        Log.d(TAG, "📤 Saving history for: " + trackName);

        spotifyApi.addHistoryRecord(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "✅ History saved successfully: " + trackName);
                    if (callback != null) {
                        callback.onSuccess();
                    }
                } else {
                    Log.e(TAG, "❌ Failed to save history. HTTP " + response.code());
                    if (callback != null) {
                        callback.onError(response.code(), "HTTP " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e(TAG, "❌ Network error while saving history: " + t.getMessage());
                if (callback != null) {
                    callback.onError(0, t.getMessage());
                }
            }
        });
    }

    /**
     * Callback cho kết quả lưu lịch sử
     */
    public interface HistorySaveCallback {
        void onSuccess();

        void onError(int code, String message);

        void onSkipped(String reason);
    }

    // ==================== Settings Management ====================

    /**
     * Kiểm tra xem lưu lịch sử có được bật không
     */
    public boolean isHistoryEnabled() {
        return prefs.getBoolean(KEY_ENABLED, DEFAULT_ENABLED);
    }

    /**
     * Bật/tắt lưu lịch sử
     */
    public void setHistoryEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply();
        Log.d(TAG, enabled ? "✅ History tracking enabled" : "⏸️ History tracking disabled");
    }

    /**
     * Lấy thời gian phát tối thiểu (giây)
     */
    public int getMinPlayDuration() {
        return prefs.getInt(KEY_MIN_DURATION, DEFAULT_MIN_DURATION);
    }

    /**
     * Đặt thời gian phát tối thiểu (giây)
     */
    public void setMinPlayDuration(int seconds) {
        prefs.edit().putInt(KEY_MIN_DURATION, seconds).apply();
        Log.d(TAG, "⏱️ Min play duration set to " + seconds + " seconds");
    }

    /**
     * Lấy khoảng thời gian để tránh duplicate (giây)
     */
    public int getDedupeWindow() {
        return prefs.getInt(KEY_DEDUPE_WINDOW, DEFAULT_DEDUPE_WINDOW);
    }

    /**
     * Đặt khoảng thời gian để tránh duplicate (giây)
     */
    public void setDedupeWindow(int seconds) {
        prefs.edit().putInt(KEY_DEDUPE_WINDOW, seconds).apply();
        Log.d(TAG, "🔁 Dedupe window set to " + seconds + " seconds");
    }

    // ==================== Duplicate Prevention ====================

    /**
     * Kiểm tra track có vừa được phát gần đây không
     */
    private boolean isRecentlyPlayed(String trackId) {
        cleanupOldEntries();
        String key = trackId + "_" + System.currentTimeMillis();
        return recentTrackIds.contains(trackId);
    }

    /**
     * Đánh dấu track đã được phát
     */
    private void markAsRecentlyPlayed(String trackId) {
        cleanupOldEntries();
        String key = trackId + "_" + System.currentTimeMillis();
        recentTrackIds.add(key);
        saveRecentTracks();

        // Tự động cleanup sau một khoảng thời gian
        scheduleCleanup();
    }

    /**
     * Xóa các entry cũ hơn dedupe window
     */
    private void cleanupOldEntries() {
        long now = System.currentTimeMillis();
        long dedupeWindowMs = getDedupeWindow() * 1000L;

        Set<String> toRemove = new HashSet<>();
        for (String key : recentTrackIds) {
            if (key.contains("_")) {
                try {
                    String[] parts = key.split("_");
                    long timestamp = Long.parseLong(parts[parts.length - 1]);
                    if (now - timestamp > dedupeWindowMs) {
                        toRemove.add(key);
                    }
                } catch (Exception e) {
                    // Invalid format, remove it
                    toRemove.add(key);
                }
            }
        }

        if (!toRemove.isEmpty()) {
            recentTrackIds.removeAll(toRemove);
            saveRecentTracks();
            Log.d(TAG, "🗑️ Cleaned up " + toRemove.size() + " old entries");
        }
    }

    /**
     * Lưu danh sách track gần đây vào SharedPreferences
     */
    private void saveRecentTracks() {
        prefs.edit().putStringSet(KEY_RECENT_TRACKS, new HashSet<>(recentTrackIds)).apply();
    }

    /**
     * Schedule cleanup sau một khoảng thời gian
     */
    private void scheduleCleanup() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupTime > 60000) { // Cleanup mỗi phút
            lastCleanupTime = now;
            cleanupOldEntries();
        }
    }

    /**
     * Xóa tất cả dữ liệu recent tracks
     */
    public void clearRecentTracks() {
        recentTrackIds.clear();
        saveRecentTracks();
        Log.d(TAG, "🗑️ All recent tracks cleared");
    }
}
