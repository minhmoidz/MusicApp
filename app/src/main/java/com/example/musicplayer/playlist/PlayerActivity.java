package com.example.musicplayer.playlist;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
//import androidx.palette.graphics.Palette;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.musicplayer.R;
import com.example.musicplayer.api.HistoryRecordRequest;
import com.example.musicplayer.api.SpotifyApi;
import com.example.musicplayer.login.LoginActivity;
import com.example.musicplayer.utils.HistoryManager;

import org.json.JSONObject;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PlayerActivity extends AppCompatActivity {

    private static final String TAG = "PlayerActivity";
    private MediaPlayer mediaPlayer;
    private ImageView imgCover, imgBackground, imgVinyl;
    private ImageButton btnPlayPause, btnBack, btnNext, btnPrevious, btnShuffle, btnRepeat, btnLike, btnDownload;
    private TextView txtTitle, txtArtist, txtLyrics, txtCurrentTime, txtTotalTime;
    private SeekBar seekBar;
    private View gradientOverlay;

    private boolean isPlaying = false;
    private boolean isShuffle = false;
    private boolean isLiked = false;
    private int repeatMode = 0;
    private Handler handler = new Handler();
    private Runnable updateSeekBar;
    private RotateAnimation rotateAnimation;

    private ArrayList<Song> playlist;
    private int currentSongIndex = 0;

    private int dominantColor = Color.parseColor("#1DB954");

    private SpotifyApi spotifyApi;
    private LoginActivity.SessionManager sessionManager;
    private HistoryManager historyManager;

    // Track playback time for accurate history recording
    private long songStartTime = 0;
    private String currentPlayingTrackId = null; // Track currently playing song

    public static class Song {
        String id;
        String title;
        String artist;
        String cover;
        String preview;
        int durationMs;

        public Song(String id, String title, String artist, String cover, String preview, int durationMs) {
            this.id = id;
            this.title = title;
            this.artist = artist;
            this.cover = cover;
            this.preview = preview;
            this.durationMs = durationMs;
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "🚀 PlayerActivity onCreate() started");
        setContentView(R.layout.activity_player);

        // ADDED: Init SessionManager and Retrofit
        Log.d(TAG, "📋 Initializing SessionManager...");
        sessionManager = new LoginActivity.SessionManager(this);

        // ADDED: Init HistoryManager
        Log.d(TAG, "📋 Initializing HistoryManager...");
        historyManager = new HistoryManager(this);
        Log.d(TAG, "✅ HistoryManager initialized. History tracking enabled: " + historyManager.isHistoryEnabled());

        // Check token validity immediately
        if (!sessionManager.isTokenValid()) {
            Log.e(TAG, "❌ Token is invalid or expired! Redirecting to login...");
            Toast.makeText(this, "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại", Toast.LENGTH_LONG).show();
            redirectToLogin();
            return;
        }

        String validToken = sessionManager.getValidAccessToken();
        if (validToken != null) {
            Log.d(TAG, "✅ Valid token found. Remaining time: " + sessionManager.getRemainingTimeSeconds() + " seconds");
        } else {
            Log.e(TAG, "❌ Could not get valid token! Redirecting to login...");
            redirectToLogin();
            return;
        }

        setupRetrofit();

        initViews();
        loadPlaylistData();
        loadCurrentSong();
        setupMediaPlayer();
        setupControls();

        Log.d(TAG, "✅ PlayerActivity onCreate() completed");
    }

    private void redirectToLogin() {
        sessionManager.clear();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }


    private void setupRetrofit() {
        Log.d(TAG, "🔧 Setting up Retrofit and OkHttpClient...");

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    // Use getValidAccessToken() to ensure we only use valid tokens
                    String token = sessionManager.getValidAccessToken();
                    okhttp3.Request.Builder builder = chain.request().newBuilder();

                    if (token != null && !token.isEmpty()) {
                        builder.header("Authorization", "Bearer " + token);
                        Log.d(TAG, "🔑 Adding Authorization header to request: " + chain.request().url());
                    } else {
                        Log.e(TAG, "❌ No valid token available for request: " + chain.request().url());
                        Log.e(TAG, "❌ This request will likely fail with 401 Unauthorized");
                    }

                    okhttp3.Request request = builder.build();
                    Log.d(TAG, "📡 Request: " + request.method() + " " + request.url());

                    okhttp3.Response response = chain.proceed(request);
                    Log.d(TAG, "📥 Response: " + response.code() + " for " + request.url());

                    // Handle 401 Unauthorized - token may have expired during app usage
                    if (response.code() == 401) {
                        Log.e(TAG, "🔴 401 Unauthorized! Token may be expired or invalid.");
                        runOnUiThread(() -> {
                            Toast.makeText(PlayerActivity.this, "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại", Toast.LENGTH_LONG).show();
                            redirectToLogin();
                        });
                    }

                    return response;
                })
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.30.28:5030/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        spotifyApi = retrofit.create(SpotifyApi.class);

        Log.d(TAG, "✅ Retrofit setup complete. spotifyApi initialized: " + (spotifyApi != null));
    }

    private void initViews() {
        imgCover = findViewById(R.id.imgCover);
        imgBackground = findViewById(R.id.imgBackground);
        imgVinyl = findViewById(R.id.imgVinyl);
        txtTitle = findViewById(R.id.txtTitle);
        txtArtist = findViewById(R.id.txtArtist);
        txtLyrics = findViewById(R.id.txtLyrics);
        txtCurrentTime = findViewById(R.id.txtCurrentTime);
        txtTotalTime = findViewById(R.id.txtTotalTime);
        seekBar = findViewById(R.id.seekBar);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnBack = findViewById(R.id.btnBack);
        btnNext = findViewById(R.id.btnNext);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnShuffle = findViewById(R.id.btnShuffle);
        btnRepeat = findViewById(R.id.btnRepeat);
        btnLike = findViewById(R.id.btnLike);
        btnDownload = findViewById(R.id.btnDownload);
        gradientOverlay = findViewById(R.id.gradientOverlay);
    }

    // UPDATED: To receive full data for history
    // UPDATED: To receive full data for history
    private void loadPlaylistData() {
        playlist = new ArrayList<>();
        ArrayList<String> titles = getIntent().getStringArrayListExtra("playlist_titles");
        ArrayList<String> artists = getIntent().getStringArrayListExtra("playlist_artists");
        ArrayList<String> covers = getIntent().getStringArrayListExtra("playlist_covers");
        ArrayList<String> previews = getIntent().getStringArrayListExtra("playlist_previews");
        ArrayList<String> ids = getIntent().getStringArrayListExtra("playlist_ids");
        ArrayList<Integer> durations = getIntent().getIntegerArrayListExtra("playlist_durations");
        currentSongIndex = getIntent().getIntExtra("current_index", 0);

        if (titles != null && ids != null) {
            for (int i = 0; i < titles.size(); i++) {
                playlist.add(new Song(ids.get(i), titles.get(i), artists.get(i), covers.get(i), previews.get(i), durations.get(i)));
            }
        }

        Log.d(TAG, "Playlist loaded: " + playlist.size() + " songs");
    }

    private void loadCurrentSong() {
        if (playlist == null || playlist.isEmpty()) {
            Toast.makeText(this, "Không có bài hát!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Song song = playlist.get(currentSongIndex);

        Log.d(TAG, "Loading: " + song.title);

        txtTitle.setText(song.title != null ? song.title : "Unknown");
        txtArtist.setText(song.artist != null ? song.artist : "Unknown");

        // Fade in animation
        txtTitle.setAlpha(0f);
        txtArtist.setAlpha(0f);
        txtTitle.animate().alpha(1f).setDuration(500).start();
        txtArtist.animate().alpha(1f).setDuration(500).setStartDelay(100).start();

        // Load album cover with Glide and extract colors
        if (song.cover != null && !song.cover.isEmpty()) {
            Glide.with(this)
                    .asBitmap()
                    .load(song.cover)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .circleCrop()
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap bitmap, Transition<? super Bitmap> transition) {
                            imgCover.setImageBitmap(bitmap);

                            // Extract dominant color from bitmap
                            int extractedColor = extractDominantColor(bitmap);

                            // Animate color change
                            animateColorChange(extractedColor);

                            // Set blurred background
                            setBlurredBackground(bitmap);
                        }

                        @Override
                        public void onLoadCleared(Drawable placeholder) {
                            imgCover.setImageDrawable(placeholder);
                        }
                    });
        } else {
            imgCover.setImageResource(android.R.drawable.ic_menu_gallery);
            imgBackground.setImageResource(android.R.color.black);
        }

        // Fetch lyrics
        if (song.artist != null && song.title != null) {
            txtLyrics.setText("Đang tải lời bài hát...");
            new Thread(() -> fetchLyrics(song.artist, song.title)).start();
        }
    }

    private void setBlurredBackground(Bitmap originalBitmap) {
        try {
            // Resize bitmap for faster blur
            int width = originalBitmap.getWidth() / 8;
            int height = originalBitmap.getHeight() / 8;
            Bitmap smallBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, false);

            Bitmap blurredBitmap = Bitmap.createBitmap(smallBitmap);

            RenderScript rs = RenderScript.create(this);
            ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
            Allocation input = Allocation.createFromBitmap(rs, smallBitmap);
            Allocation output = Allocation.createFromBitmap(rs, blurredBitmap);

            blur.setRadius(25f);
            blur.setInput(input);
            blur.forEach(output);
            output.copyTo(blurredBitmap);

            rs.destroy();

            imgBackground.setImageBitmap(blurredBitmap);
            imgBackground.setAlpha(0f);
            imgBackground.animate().alpha(0.4f).setDuration(500).start();

        } catch (Exception e) {
            Log.e(TAG, "Blur error: " + e.getMessage());
        }
    }

    // Extract dominant color from bitmap manually
    private int extractDominantColor(Bitmap bitmap) {
        try {
            // Sample pixels from center area
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int centerX = width / 2;
            int centerY = height / 2;

            // Sample size
            int sampleSize = Math.min(width, height) / 4;

            long redSum = 0;
            long greenSum = 0;
            long blueSum = 0;
            int pixelCount = 0;

            // Sample pixels in a grid pattern
            for (int x = centerX - sampleSize/2; x < centerX + sampleSize/2; x += 5) {
                for (int y = centerY - sampleSize/2; y < centerY + sampleSize/2; y += 5) {
                    if (x >= 0 && x < width && y >= 0 && y < height) {
                        int pixel = bitmap.getPixel(x, y);

                        int red = Color.red(pixel);
                        int green = Color.green(pixel);
                        int blue = Color.blue(pixel);

                        // Skip very dark or very light pixels
                        int brightness = (red + green + blue) / 3;
                        if (brightness > 30 && brightness < 225) {
                            redSum += red;
                            greenSum += green;
                            blueSum += blue;
                            pixelCount++;
                        }
                    }
                }
            }

            if (pixelCount > 0) {
                int avgRed = (int)(redSum / pixelCount);
                int avgGreen = (int)(greenSum / pixelCount);
                int avgBlue = (int)(blueSum / pixelCount);

                // Boost saturation for more vibrant color
                float[] hsv = new float[3];
                Color.RGBToHSV(avgRed, avgGreen, avgBlue, hsv);
                hsv[1] = Math.min(1.0f, hsv[1] * 1.3f); // Increase saturation
                hsv[2] = Math.min(1.0f, hsv[2] * 1.1f); // Slightly increase brightness

                return Color.HSVToColor(hsv);
            }

        } catch (Exception e) {
            Log.e(TAG, "Color extraction error: " + e.getMessage());
        }

        // Default color if extraction fails
        return Color.parseColor("#1DB954");
    }

    private void animateColorChange(int newColor) {
        ValueAnimator colorAnim = ValueAnimator.ofArgb(dominantColor, newColor);
        colorAnim.setDuration(800);
        colorAnim.addUpdateListener(animator -> {
            int color = (int) animator.getAnimatedValue();
            if (gradientOverlay != null) {
                gradientOverlay.setBackgroundColor(adjustAlpha(color, 0.3f));
            }
            seekBar.getProgressDrawable().setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
            seekBar.getThumb().setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
        });
        colorAnim.start();
        dominantColor = newColor;
    }

    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    private void setupMediaPlayer() {
        Song song = playlist.get(currentSongIndex);
        String preview = song.preview;

        Log.d(TAG, "Setup MediaPlayer: " + preview);

        if (preview == null || preview.isEmpty()) {
            Toast.makeText(this, "Không có URL nhạc! Đang chuyển bài...", Toast.LENGTH_SHORT).show();
            handleSongCompletion(); // Auto skip to next song
            return;
        }

        try {
            if (mediaPlayer != null) {
                mediaPlayer.reset();
            } else {
                mediaPlayer = new MediaPlayer();
            }

            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
            );

            mediaPlayer.setDataSource(preview);

            mediaPlayer.setOnPreparedListener(mp -> {
                Log.d(TAG, "✅ Ready to play!");
                int duration = mp.getDuration();

                txtTotalTime.setText(formatTime(duration));
                seekBar.setMax(duration);

                mp.start();
                isPlaying = true;
                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                
                // Track song start time for accurate play duration
                songStartTime = System.currentTimeMillis();
                currentPlayingTrackId = song.id; // Track current song
                Log.d(TAG, "⏱️ Song playback started: " + song.title + " at: " + songStartTime);

                // Scale animation for play button
                btnPlayPause.setScaleX(0.8f);
                btnPlayPause.setScaleY(0.8f);
                btnPlayPause.animate().scaleX(1f).scaleY(1f).setDuration(300).start();

                startSeekBarUpdater();
                startDiscAnimation();

                Toast.makeText(this, "♫ " + song.title, Toast.LENGTH_SHORT).show();
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "❌ Error: " + what);
                Toast.makeText(this, "Lỗi phát nhạc!", Toast.LENGTH_LONG).show();
                return true;
            });

            mediaPlayer.setOnCompletionListener(mp -> {
`                Log.d(TAG, "Song completed");
                // Save history when song completes
                saveHistoryOnComplete(song);
                // Reset tracking after save
                songStartTime = 0;
                currentPlayingTrackId = null;
                handleSongCompletion();
            });

            mediaPlayer.prepareAsync();

        } catch (Exception e) {
            Log.e(TAG, "Exception: " + e.getMessage());
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Lưu lịch sử khi bài hát kết thúc hoặc chuyển bài
     * Tính toán thời gian phát thực tế
     * ⭐ Sử dụng auto-save API (recommended)
     */
    private void saveHistoryOnComplete(Song song) {
        if (song == null || song.id == null || song.id.isEmpty()) {
            Log.w(TAG, "⚠️ Cannot save history, invalid song data");
            return;
        }

        // Check if spotifyApi and historyManager are initialized
        if (spotifyApi == null || historyManager == null) {
            Log.e(TAG, "❌ API or HistoryManager not initialized");
            return;
        }

        // Check token validity
        String validToken = sessionManager.getValidAccessToken();
        if (validToken == null) {
            Log.w(TAG, "⚠️ No valid token, cannot save history");
            return;
        }

        // Calculate actual play duration
        if (songStartTime <= 0) {
            Log.w(TAG, "⚠️ No start time, skip saving");
            return;
        }
        
        long actualPlayTimeMs = System.currentTimeMillis() - songStartTime;
        final int playDurationSeconds = (int) (actualPlayTimeMs / 1000);
        Log.d(TAG, "⏱️ Actual play duration: " + playDurationSeconds + " seconds for: " + song.title);

        // Store song ID for duplicate check but DON'T clear yet
        final String trackIdToSave = song.id;
        
        // Skip if this exact song was just saved (check against last saved, not current playing)
        if (currentPlayingTrackId != null && !currentPlayingTrackId.equals(trackIdToSave)) {
            Log.d(TAG, "⏭️ Skip saving, different song is now playing");
            return;
        }

        Log.d(TAG, "💾 Attempting to save history for: " + song.title + " (ID: " + trackIdToSave + ")");

        // ⭐ Use auto-save API (recommended) - backend automatically fetches track info from Spotify
        historyManager.saveHistoryAutoSave(
                spotifyApi,
                trackIdToSave,
                playDurationSeconds,
                new HistoryManager.HistorySaveCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "✅ History auto-saved successfully for: " + song.title);
                    }

                    @Override
                    public void onError(int code, String message) {
                        Log.e(TAG, "❌ Failed to auto-save history: " + code + " - " + message);
                        
                        // If auto-save fails, try manual fallback with full data
                        if (code != 401) { // Don't retry if token expired
                            saveHistoryManualFallback(song, playDurationSeconds);
                        } else {
                            runOnUiThread(() -> {
                                Toast.makeText(PlayerActivity.this,
                                        "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại",
                                        Toast.LENGTH_LONG).show();
                                redirectToLogin();
                            });
                        }
                    }

                    @Override
                    public void onSkipped(String reason) {
                        Log.d(TAG, "⏭️ History save skipped: " + reason);
                    }
                }
        );
    }
    
    private void saveHistoryManualFallback(Song song, int playDurationSeconds) {
        String trackName = song.title != null ? song.title : "Unknown";
        String artistName = song.artist != null ? song.artist : "Unknown";
        String album = "Unknown";
        int durationMs = song.durationMs > 0 ? song.durationMs : 30000;

        Log.d(TAG, "🔄 Trying manual fallback for: " + trackName);

        historyManager.saveHistory(
                spotifyApi,
                song.id,
                trackName,
                artistName,
                album,
                durationMs,
                playDurationSeconds,
                new HistoryManager.HistorySaveCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "✅ History saved via manual fallback");
                    }

                    @Override
                    public void onError(int code, String message) {
                        Log.e(TAG, "❌ Manual fallback also failed: " + code + " - " + message);
                    }

                    @Override
                    public void onSkipped(String reason) {
                        Log.d(TAG, "⏭️ Manual fallback skipped: " + reason);
                    }
                }
        );
    }

    private void handleSongCompletion() {
    if (repeatMode == 2) {
        playCurrentSong();
    } else if (repeatMode == 1 || currentSongIndex < playlist.size() - 1) {
        playNext();
    } else {
        isPlaying = false;
        btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
        stopDiscAnimation();
        seekBar.setProgress(0);
        txtCurrentTime.setText("00:00");
    }
}

private void setupControls() {
    // Play/Pause
    btnPlayPause.setOnClickListener(v -> {
        if (mediaPlayer == null) return;

        try {
            if (isPlaying) {
                mediaPlayer.pause();
                stopDiscAnimation();
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
            } else {
                mediaPlayer.start();
                startDiscAnimation();
                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            }

            // Pulse animation
            btnPlayPause.animate()
                    .scaleX(0.85f).scaleY(0.85f)
                    .setDuration(100)
                    .withEndAction(() ->
                            btnPlayPause.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    ).start();

            isPlaying = !isPlaying;
        } catch (Exception e) {
            Log.e(TAG, "Error: " + e.getMessage());
        }
    });

    // Next button
    btnNext.setOnClickListener(v -> {
        animateButton(v);
        playNext();
    });

    // Previous button
    btnPrevious.setOnClickListener(v -> {
        animateButton(v);
        playPrevious();
    });

    // Shuffle button
    btnShuffle.setOnClickListener(v -> {
        isShuffle = !isShuffle;
        animateButton(v);

        if (isShuffle) {
            btnShuffle.setColorFilter(dominantColor);
            Toast.makeText(this, "🔀 Phát ngẫu nhiên", Toast.LENGTH_SHORT).show();
        } else {
            btnShuffle.setColorFilter(Color.WHITE);
            Toast.makeText(this, "▶ Phát tuần tự", Toast.LENGTH_SHORT).show();
        }
    });

    // Repeat button
    btnRepeat.setOnClickListener(v -> {
        repeatMode = (repeatMode + 1) % 3;
        animateButton(v);
        updateRepeatButton();
    });

    // Like button
    btnLike.setOnClickListener(v -> {
        isLiked = !isLiked;
        animateButton(v);

        if (isLiked) {
            btnLike.setImageResource(android.R.drawable.btn_star_big_on);
            btnLike.setColorFilter(Color.RED);
            Toast.makeText(this, "❤️ Đã thêm vào yêu thích", Toast.LENGTH_SHORT).show();
        } else {
            btnLike.setImageResource(android.R.drawable.btn_star_big_off);
            btnLike.setColorFilter(Color.WHITE);
            Toast.makeText(this, "🤍 Đã bỏ yêu thích", Toast.LENGTH_SHORT).show();
        }
    });

    // Download button
    btnDownload.setOnClickListener(v -> {
        animateButton(v);
        Toast.makeText(this, "⬇️ Tính năng tải xuống đang phát triển", Toast.LENGTH_SHORT).show();
    });

    // Back button
    btnBack.setOnClickListener(v -> finish());

    // SeekBar
    seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser && mediaPlayer != null) {
                try {
                    mediaPlayer.seekTo(progress);
                    txtCurrentTime.setText(formatTime(progress));
                } catch (Exception e) {
                    Log.e(TAG, "Seek error: " + e.getMessage());
                }
            }
        }
        @Override public void onStartTrackingTouch(SeekBar seekBar) {}
        @Override public void onStopTrackingTouch(SeekBar seekBar) {}
    });
}

private void animateButton(View view) {
    view.animate()
            .scaleX(0.8f).scaleY(0.8f)
            .setDuration(100)
            .withEndAction(() ->
                    view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            ).start();
}

private void playNext() {
    if (playlist.size() <= 1) {
        Toast.makeText(this, "Không có bài tiếp theo", Toast.LENGTH_SHORT).show();
        return;
    }

    // Save history for current song before switching
    Song currentSong = playlist.get(currentSongIndex);
    saveHistoryOnComplete(currentSong);
    
    // Reset tracking for next song
    songStartTime = 0;
    currentPlayingTrackId = null;

    if (isShuffle) {
        int randomIndex;
        do {
            randomIndex = (int) (Math.random() * playlist.size());
        } while (randomIndex == currentSongIndex && playlist.size() > 1);
        currentSongIndex = randomIndex;
    } else {
        currentSongIndex = (currentSongIndex + 1) % playlist.size();
    }

    // Slide out animation
    imgCover.animate().alpha(0f).setDuration(200).withEndAction(() -> {
        loadCurrentSong();
        setupMediaPlayer();
        imgCover.animate().alpha(1f).setDuration(300).start();
    }).start();
}

private void playPrevious() {
    if (playlist.size() <= 1) {
        Toast.makeText(this, "Không có bài trước", Toast.LENGTH_SHORT).show();
        return;
    }

    // Save history for current song before switching
    Song currentSong = playlist.get(currentSongIndex);
    saveHistoryOnComplete(currentSong);
    
    // Reset tracking for next song
    songStartTime = 0;
    currentPlayingTrackId = null;

    if (mediaPlayer != null && mediaPlayer.getCurrentPosition() > 3000) {
        mediaPlayer.seekTo(0);
        return;
    }

    currentSongIndex = (currentSongIndex - 1 + playlist.size()) % playlist.size();

    imgCover.animate().alpha(0f).setDuration(200).withEndAction(() -> {
        loadCurrentSong();
        setupMediaPlayer();
        imgCover.animate().alpha(1f).setDuration(300).start();
    }).start();
}

private void playCurrentSong() {
    loadCurrentSong();
    setupMediaPlayer();
}

private void updateRepeatButton() {
    switch (repeatMode) {
        case 0:
            btnRepeat.setColorFilter(Color.WHITE);
            Toast.makeText(this, "🔁 Tắt lặp lại", Toast.LENGTH_SHORT).show();
            break;
        case 1:
            btnRepeat.setColorFilter(dominantColor);
            Toast.makeText(this, "🔁 Lặp lại tất cả", Toast.LENGTH_SHORT).show();
            break;
        case 2:
            btnRepeat.setColorFilter(Color.parseColor("#FF6B35"));
            Toast.makeText(this, "🔂 Lặp lại một bài", Toast.LENGTH_SHORT).show();
            break;
    }
}

private void fetchLyrics(String artist, String title) {
    try {
        String apiUrl = "https://lyrics.lewdhutao.tech/?title=" +
                title.replace(" ", "%20") + "&artist=" + artist.replace(" ", "%20");

        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        if (conn.getResponseCode() == 200) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            JSONObject json = new JSONObject(sb.toString());
            String lyrics = json.optString("lyrics", "Không tìm thấy lời bài hát");

            runOnUiThread(() -> {
                txtLyrics.setText(lyrics);
                txtLyrics.setAlpha(0f);
                txtLyrics.animate().alpha(1f).setDuration(500).start();
            });
        }
    } catch (Exception e) {
        Log.e(TAG, "Lyrics error: " + e.getMessage());
        runOnUiThread(() -> txtLyrics.setText("Không tải được lời bài hát"));
    }
}

private void startSeekBarUpdater() {
    updateSeekBar = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && isPlaying) {
                try {
                    int currentPos = mediaPlayer.getCurrentPosition();
                    seekBar.setProgress(currentPos);
                    txtCurrentTime.setText(formatTime(currentPos));
                    handler.postDelayed(this, 500);
                } catch (Exception e) {
                    Log.e(TAG, "Update error: " + e.getMessage());
                }
            }
        }
    };
    handler.post(updateSeekBar);
}

private String formatTime(int millis) {
    long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
    long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
    return String.format("%02d:%02d", minutes, seconds);
}

private void startDiscAnimation() {
    if (rotateAnimation == null) {
        rotateAnimation = new RotateAnimation(
                0f, 360f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        rotateAnimation.setDuration(20000);
        rotateAnimation.setRepeatCount(Animation.INFINITE);
        rotateAnimation.setInterpolator(new LinearInterpolator());
    }
    imgCover.startAnimation(rotateAnimation);
}

private void stopDiscAnimation() {
    if (imgCover != null) {
        imgCover.clearAnimation();
    }
}

@Override
protected void onDestroy() {
    super.onDestroy();

    // Save history for current playing song before destroying
    if (playlist != null && !playlist.isEmpty() && currentSongIndex >= 0 && currentSongIndex < playlist.size()) {
        Song currentSong = playlist.get(currentSongIndex);
        saveHistoryOnComplete(currentSong);
        // Reset tracking
        songStartTime = 0;
        currentPlayingTrackId = null;
    }

    if (mediaPlayer != null) {
        try {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.release();
        } catch (Exception e) {
            Log.e(TAG, "Release error: " + e.getMessage());
        }
        mediaPlayer = null;
    }
    if (handler != null) {
        handler.removeCallbacks(updateSeekBar);
    }
}

    @Override
    protected void onPause() {
        super.onPause();

        // Pause playback when app goes to background
        if (mediaPlayer != null && isPlaying) {
            mediaPlayer.pause();
            isPlaying = false;
            stopDiscAnimation();
            btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        
        // Save history when app goes to background for a longer time
        if (playlist != null && !playlist.isEmpty() && currentSongIndex >= 0 && currentSongIndex < playlist.size()) {
            Song currentSong = playlist.get(currentSongIndex);
            if (songStartTime > 0) {
                // Save current progress
                Log.d(TAG, "💾 App stopped, saving history...");
                saveHistoryOnComplete(currentSong);
                // Reset tracking
                songStartTime = 0;
                currentPlayingTrackId = null;
            }
        }
    }
}