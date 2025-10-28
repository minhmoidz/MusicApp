package com.example.myapplication.playlist;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
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

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.myapplication.R;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

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

    public static class Song {
        String title;
        String artist;
        String cover;
        String preview;

        public Song(String title, String artist, String cover, String preview) {
            this.title = title;
            this.artist = artist;
            this.cover = cover;
            this.preview = preview;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        initViews();
        loadPlaylistData();
        loadCurrentSong();
        setupMediaPlayer();
        setupControls();
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

    private void loadPlaylistData() {
        playlist = new ArrayList<>();

        String title = getIntent().getStringExtra("title");
        String artist = getIntent().getStringExtra("artist");
        String cover = getIntent().getStringExtra("cover");
        String preview = getIntent().getStringExtra("preview");

        ArrayList<String> titles = getIntent().getStringArrayListExtra("playlist_titles");
        ArrayList<String> artists = getIntent().getStringArrayListExtra("playlist_artists");
        ArrayList<String> covers = getIntent().getStringArrayListExtra("playlist_covers");
        ArrayList<String> previews = getIntent().getStringArrayListExtra("playlist_previews");
        currentSongIndex = getIntent().getIntExtra("current_index", 0);

        if (titles != null && artists != null && covers != null && previews != null) {
            for (int i = 0; i < titles.size(); i++) {
                playlist.add(new Song(titles.get(i), artists.get(i), covers.get(i), previews.get(i)));
            }
        } else {
            playlist.add(new Song(title, artist, cover, preview));
            currentSongIndex = 0;
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
                        public void onResourceReady(Bitmap bitmap, Transition<? super Bitmap> transition) {
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
            Toast.makeText(this, "Không có URL nhạc!", Toast.LENGTH_LONG).show();
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
                Log.d(TAG, "Song completed");
                handleSongCompletion();
            });

            mediaPlayer.prepareAsync();

        } catch (Exception e) {
            Log.e(TAG, "Exception: " + e.getMessage());
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
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
        if (mediaPlayer != null && isPlaying) {
            mediaPlayer.pause();
            isPlaying = false;
            stopDiscAnimation();
            btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
        }
    }
}