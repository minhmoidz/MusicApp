package com.example.myapplication;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.ArrayList;

public class MusicService extends Service {
    private static final String TAG = "MusicService";
    private static final String CHANNEL_ID = "music_channel";
    private static final int NOTIFICATION_ID = 1;

    private MediaPlayer mediaPlayer;
    private ArrayList<Song> playlist = new ArrayList<>();
    private int currentSongIndex = 0;
    private boolean isPlaying = false;
    private boolean isShuffle = false;
    private int repeatMode = 0; // 0: no repeat, 1: repeat all, 2: repeat one

    private final IBinder binder = new MusicBinder();
    private MusicServiceCallback callback;

    /** Interface cho callback giữa Service và Activity */
    public interface MusicServiceCallback {
        void onPlaybackStateChanged(boolean isPlaying);
        void onSongChanged(Song song, int position);
        void onProgressChanged(int currentPosition, int duration);
    }

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Log.d(TAG, "✅ MusicService created");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setCallback(MusicServiceCallback callback) {
        this.callback = callback;
    }

    /**
     * Gán danh sách phát + chỉ định vị trí bắt đầu phát
     */
    public void setPlaylist(ArrayList<Song> playlist, int startIndex) {
        this.playlist = new ArrayList<>(playlist);
        this.currentSongIndex = startIndex;
        playSong(startIndex);
    }

    /**
     * Gán danh sách phát + callback listener (tùy chọn)
     * — tránh lỗi nếu bạn gọi setPlaylist(list, (song)->{...})
     */
    public void setPlaylist(ArrayList<Song> playlist, MusicServiceCallback callback) {
        this.playlist = new ArrayList<>(playlist);
        this.currentSongIndex = 0;
        this.callback = callback;
        playSong(0);
    }

    public void playSong(int index) {
        if (playlist == null || playlist.isEmpty() || index < 0 || index >= playlist.size()) {
            Log.e(TAG, "⚠️ Invalid playlist or index");
            return;
        }

        currentSongIndex = index;
        Song song = playlist.get(currentSongIndex);

        try {
            if (mediaPlayer != null) {
                mediaPlayer.reset();
            } else {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build()
                );

                mediaPlayer.setOnCompletionListener(mp -> handleSongCompletion());
                mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                    Log.e(TAG, "MediaPlayer error: " + what);
                    return true;
                });
            }

            mediaPlayer.setDataSource(song.audio);
            mediaPlayer.prepareAsync();

            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                isPlaying = true;
                updateNotification();
                if (callback != null) {
                    callback.onPlaybackStateChanged(true);
                    callback.onSongChanged(song, currentSongIndex);
                }
                Log.d(TAG, "🎵 Playing: " + song.title);
            });

        } catch (Exception e) {
            Log.e(TAG, "❌ Error playing song: " + e.getMessage());
        }
    }

    private void handleSongCompletion() {
        if (repeatMode == 2) {
            playSong(currentSongIndex);
        } else if (repeatMode == 1 || currentSongIndex < playlist.size() - 1) {
            playNext();
        } else {
            isPlaying = false;
            if (callback != null) callback.onPlaybackStateChanged(false);
        }
    }

    public void playPause() {
        if (mediaPlayer == null) return;

        try {
            if (isPlaying) {
                mediaPlayer.pause();
                isPlaying = false;
            } else {
                mediaPlayer.start();
                isPlaying = true;
            }
            updateNotification();
            if (callback != null) callback.onPlaybackStateChanged(isPlaying);
        } catch (Exception e) {
            Log.e(TAG, "Error play/pause: " + e.getMessage());
        }
    }

    public void playNext() {
        if (playlist.size() <= 1) return;

        if (isShuffle) {
            int randomIndex;
            do {
                randomIndex = (int) (Math.random() * playlist.size());
            } while (randomIndex == currentSongIndex && playlist.size() > 1);
            playSong(randomIndex);
        } else {
            playSong((currentSongIndex + 1) % playlist.size());
        }
    }

    public void playPrevious() {
        if (playlist.size() <= 1) return;
        playSong((currentSongIndex - 1 + playlist.size()) % playlist.size());
    }

    public void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        }
    }

    public int getCurrentPosition() {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
    }

    public int getDuration() {
        return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public Song getCurrentSong() {
        if (playlist != null && !playlist.isEmpty()
                && currentSongIndex >= 0 && currentSongIndex < playlist.size()) {
            return playlist.get(currentSongIndex);
        }
        return null;
    }

    public int getCurrentSongIndex() {
        return currentSongIndex;
    }

    public ArrayList<Song> getPlaylist() {
        return new ArrayList<>(playlist);
    }

    public void setShuffle(boolean shuffle) {
        this.isShuffle = shuffle;
    }

    public boolean isShuffle() {
        return isShuffle;
    }

    public void setRepeatMode(int mode) {
        this.repeatMode = mode;
    }

    public int getRepeatMode() {
        return repeatMode;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Music Player",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void updateNotification() {
        Song song = getCurrentSong();
        if (song == null) return;

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(song.title)
                .setContentText(song.artist)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        Log.d(TAG, "🛑 MusicService destroyed");
    }
}
