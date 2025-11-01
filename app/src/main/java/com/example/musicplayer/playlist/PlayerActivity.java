package com.example.musicplayer.playlist;

import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.musicplayer.R;
import com.example.musicplayer.profile.FavoritesManager;

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

    private static ArrayList<Song> playlist = new ArrayList<>();
    private static int currentSongIndex = 0;

    private int dominantColor = Color.parseColor("#1DB954");

    // Favorites Manager
    private FavoritesManager favoritesManager;

    // Lớp Song lồng nhau (nested static class)
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

        // Đảm bảo bạn đã thay R.layout.activity_player bằng tên file XML đúng
        setContentView(R.layout.activity_player);

        // Khởi tạo
        favoritesManager = new FavoritesManager(this);

        // Gọi các hàm setup
        initViews();
        loadPlaylistData();
        loadCurrentSong();
        setupMediaPlayer();
        setupControls();
        registerControlReceiver();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // Luôn cập nhật intent mới

        // Dừng nhạc cũ (nếu có)
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.reset();
            } catch (Exception e) {
                Log.e(TAG, "Reset error: " + e.getMessage());
            }
        }

        // Tải dữ liệu (hàm này đã được sửa để không xóa hàng đợi)
        loadPlaylistData();

        // Chỉ load và phát NẾU hàng đợi không rỗng
        if (!playlist.isEmpty()) {
            loadCurrentSong();
            setupMediaPlayer();
        } else {
            // Nếu vì lý do nào đó mà playlist rỗng, đóng activity
            Log.e(TAG, "Hàng đợi rỗng sau khi onNewIntent, đóng Player.");
            Toast.makeText(this, "Không có nhạc để phát", Toast.LENGTH_SHORT).show();
            finish();
        }
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
        // Lấy dữ liệu từ Intent mới
        ArrayList<String> newTitles = getIntent().getStringArrayListExtra("playlist_titles");

        // QUAN TRỌNG: Chỉ xóa và tải lại hàng đợi NẾU một playlist MỚI được gửi
        if (newTitles != null && !newTitles.isEmpty()) {
            Log.d(TAG, "Phát playlist mới! Xóa hàng đợi cũ.");

            // Xóa hàng đợi static cũ
            playlist.clear();

            // Lấy các danh sách còn lại
            ArrayList<String> newArtists = getIntent().getStringArrayListExtra("playlist_artists");
            ArrayList<String> newCovers = getIntent().getStringArrayListExtra("playlist_covers");
            ArrayList<String> newPreviews = getIntent().getStringArrayListExtra("playlist_previews");

            // Đặt lại vị trí bài hát
            currentSongIndex = getIntent().getIntExtra("current_index", 0);

            if (newArtists != null && newCovers != null && newPreviews != null) {
                for (int i = 0; i < newTitles.size(); i++) {
                    playlist.add(new Song(
                            newTitles.get(i),
                            newArtists.get(i),
                            newCovers.get(i),
                            newPreviews.get(i)
                    ));
                }
            }
        } else {
            // Nếu không có playlist mới (ví dụ: chỉ bấm nút btnQueue)
            // -> KHÔNG LÀM GÌ CẢ, cứ giữ nguyên hàng đợi static hiện tại
            Log.d(TAG, "Mở lại Player. Giữ nguyên " + playlist.size() + " bài trong hàng đợi.");
        }

        // Xử lý trường hợp chỉ gửi 1 bài hát (từ code cũ của bạn)
        String title = getIntent().getStringExtra("title");
        if (playlist.isEmpty() && title != null) {
            playlist.add(new Song(
                    title,
                    getIntent().getStringExtra("artist"),
                    getIntent().getStringExtra("cover"),
                    getIntent().getStringExtra("preview")
            ));
            currentSongIndex = 0;
        }

        Log.d(TAG, "Hàng đợi hiện tại có: " + playlist.size() + " bài hát.");
    }

    private void loadCurrentSong() {
        if (playlist == null || playlist.isEmpty() || currentSongIndex >= playlist.size()) {
            Toast.makeText(this, "Không có bài hát!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Song song = playlist.get(currentSongIndex);
        Log.d(TAG, "Loading: " + song.title);

        txtTitle.setText(song.title != null ? song.title : "Unknown");
        txtArtist.setText(song.artist != null ? song.artist : "Unknown");

        updateLikeButton();

        txtTitle.setAlpha(0f);
        txtArtist.setAlpha(0f);
        txtTitle.animate().alpha(1f).setDuration(500).start();
        txtArtist.animate().alpha(1f).setDuration(500).setStartDelay(100).start();

        if (song.cover != null && !song.cover.isEmpty()) {
            Glide.with(this)
                    .asBitmap()
                    .load(song.cover)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .circleCrop()
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
                            imgCover.setImageBitmap(bitmap);
                            int extractedColor = extractDominantColor(bitmap);
                            animateColorChange(extractedColor);
                            setBlurredBackground(bitmap);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            imgCover.setImageDrawable(placeholder);
                        }
                    });
        } else {
            imgCover.setImageResource(android.R.drawable.ic_menu_gallery);
            imgBackground.setImageResource(android.R.color.black);
        }

        if (song.artist != null && song.title != null) {
            txtLyrics.setText("Đang tải lời bài hát...");
            new Thread(() -> fetchLyrics(song.artist, song.title)).start();
        }

        sendUpdateToMain();
    }

    private void updateLikeButton() {
        if (favoritesManager == null) {
            Log.e(TAG, "FavoritesManager is null in updateLikeButton!");
            return;
        }
        Song song = playlist.get(currentSongIndex);
        isLiked = favoritesManager.isFavorite(song.title, song.artist);

        if (isLiked) {
            btnLike.setImageResource(android.R.drawable.btn_star_big_on);
            btnLike.setColorFilter(Color.RED);
        } else {
            btnLike.setImageResource(android.R.drawable.btn_star_big_off);
            btnLike.setColorFilter(Color.WHITE);
        }
    }

    private void setBlurredBackground(Bitmap originalBitmap) {
        // (Code giữ nguyên)
        try {
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

    private int extractDominantColor(Bitmap bitmap) {
        // (Code giữ nguyên)
        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int centerX = width / 2;
            int centerY = height / 2;
            int sampleSize = Math.min(width, height) / 4;
            long redSum = 0, greenSum = 0, blueSum = 0;
            int pixelCount = 0;
            for (int x = centerX - sampleSize/2; x < centerX + sampleSize/2; x += 5) {
                for (int y = centerY - sampleSize/2; y < centerY + sampleSize/2; y += 5) {
                    if (x >= 0 && x < width && y >= 0 && y < height) {
                        int pixel = bitmap.getPixel(x, y);
                        int red = Color.red(pixel);
                        int green = Color.green(pixel);
                        int blue = Color.blue(pixel);
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
                float[] hsv = new float[3];
                Color.RGBToHSV(avgRed, avgGreen, avgBlue, hsv);
                hsv[1] = Math.min(1.0f, hsv[1] * 1.3f);
                hsv[2] = Math.min(1.0f, hsv[2] * 1.1f);
                return Color.HSVToColor(hsv);
            }
        } catch (Exception e) {
            Log.e(TAG, "Color extraction error: " + e.getMessage());
        }
        return Color.parseColor("#1DB954");
    }

    private void animateColorChange(int newColor) {
        // (Code giữ nguyên)
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
        // (Code giữ nguyên)
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    private void setupMediaPlayer() {
        if (playlist == null || playlist.isEmpty()) return; // Thêm kiểm tra

        Song song = playlist.get(currentSongIndex);
        String preview = song.preview;
        Log.d(TAG, "Setup MediaPlayer: " + preview);

        if (preview == null || preview.isEmpty()) {
            Toast.makeText(this, "Không có URL nhạc!", Toast.LENGTH_LONG).show();
            // Tự động chuyển bài tiếp theo nếu bài hiện tại lỗi
            handleSongCompletion();
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
                btnPlayPause.setScaleX(0.8f);
                btnPlayPause.setScaleY(0.8f);
                btnPlayPause.animate().scaleX(1f).scaleY(1f).setDuration(300).start();
                startSeekBarUpdater();
                startDiscAnimation();
                Toast.makeText(this, "♫ " + song.title, Toast.LENGTH_SHORT).show();
                sendUpdateToMain();
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "❌ Error: " + what);
                Toast.makeText(this, "Lỗi phát nhạc, chuyển bài...", Toast.LENGTH_LONG).show();
                // Tự động chuyển bài tiếp theo
                handleSongCompletion();
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
        } else if (isShuffle) { // Thêm logic shuffle vào đây
            playNext(); // playNext đã bao gồm logic shuffle
        } else if (repeatMode == 1) {
            playNext(); // playNext đã bao gồm logic lặp lại playlist
        } else if (currentSongIndex < playlist.size() - 1) {
            // Trường hợp phát tuần tự và chưa hết playlist
            playNext();
        } else {
            // Phát tuần tự và đã hết playlist
            isPlaying = false;
            btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
            stopDiscAnimation();
            if(mediaPlayer != null) {
                mediaPlayer.seekTo(0);
            }
            seekBar.setProgress(0);
            txtCurrentTime.setText("00:00");
            sendUpdateToMain();
        }
    }

    private void setupControls() {
        // (Code giữ nguyên)
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnNext.setOnClickListener(v -> { animateButton(v); playNext(); });
        btnPrevious.setOnClickListener(v -> { animateButton(v); playPrevious(); });

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

        btnRepeat.setOnClickListener(v -> {
            repeatMode = (repeatMode + 1) % 3;
            animateButton(v);
            updateRepeatButton();
        });

        btnLike.setOnClickListener(v -> {
            Song song = playlist.get(currentSongIndex);
            if (isLiked) {
                boolean removed = favoritesManager.removeFavorite(song.title, song.artist);
                if (removed) {
                    isLiked = false;
                    btnLike.setImageResource(android.R.drawable.btn_star_big_off);
                    btnLike.setColorFilter(Color.WHITE);
                    Toast.makeText(this, "🤍 Đã bỏ yêu thích", Toast.LENGTH_SHORT).show();
                }
            } else {
                boolean added = favoritesManager.addFavorite(song.title, song.artist, song.cover, song.preview);
                if (added) {
                    isLiked = true;
                    btnLike.setImageResource(android.R.drawable.btn_star_big_on);
                    btnLike.setColorFilter(Color.RED);
                    Toast.makeText(this, "❤️ Đã thêm vào yêu thích", Toast.LENGTH_SHORT).show();
                }
            }
            animateButton(v);
            v.animate().scaleX(1.3f).scaleY(1.3f).setDuration(150)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(150).start()).start();
        });

        btnDownload.setOnClickListener(v -> {
            animateButton(v);
            Toast.makeText(this, "⬇️ Tính năng tải xuống đang phát triển", Toast.LENGTH_SHORT).show();
        });

        btnBack.setOnClickListener(v -> { moveTaskToBack(true); });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    try {
                        mediaPlayer.seekTo(progress);
                        txtCurrentTime.setText(formatTime(progress));
                    } catch (Exception e) { Log.e(TAG, "Seek error: " + e.getMessage()); }
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void togglePlayPause() {
        // (Code giữ nguyên)
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
            btnPlayPause.animate().scaleX(0.85f).scaleY(0.85f).setDuration(100)
                    .withEndAction(() -> btnPlayPause.animate().scaleX(1f).scaleY(1f).setDuration(100).start()).start();
            isPlaying = !isPlaying;
            sendUpdateToMain();
        } catch (Exception e) {
            Log.e(TAG, "Error: " + e.getMessage());
        }
    }

    private void animateButton(View view) {
        // (Code giữ nguyên)
        view.animate().scaleX(0.8f).scaleY(0.8f).setDuration(100)
                .withEndAction(() -> view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()).start();
    }

    private void playNext() {
        if (playlist.size() <= 0) return;

        if (isShuffle) {
            int randomIndex;
            do {
                randomIndex = (int) (Math.random() * playlist.size());
            } while (randomIndex == currentSongIndex && playlist.size() > 1);
            currentSongIndex = randomIndex;
        } else {
            // Lặp lại playlist (mode 1) hoặc chuyển bài tuần tự
            currentSongIndex = (currentSongIndex + 1) % playlist.size();
        }

        imgCover.animate().alpha(0f).setDuration(200).withEndAction(() -> {
            loadCurrentSong();
            setupMediaPlayer();
            imgCover.animate().alpha(1f).setDuration(300).start();
        }).start();
    }

    private void playPrevious() {
        if (playlist.size() <= 0) return;

        if (mediaPlayer != null && mediaPlayer.getCurrentPosition() > 3000) {
            mediaPlayer.seekTo(0);
            return;
        }

        if (isShuffle) {
            // Nút Previous trong chế độ shuffle cũng chọn ngẫu nhiên
            playNext();
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
        // Dùng để lặp lại 1 bài
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(0);
            mediaPlayer.start();
        } else {
            setupMediaPlayer();
        }
    }

    private void updateRepeatButton() {
        // (Code giữ nguyên)
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
        // (Code giữ nguyên)
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
                while ((line = reader.readLine()) != null) { sb.append(line); }
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
        // (Code giữ nguyên)
        updateSeekBar = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && isPlaying) {
                    try {
                        int currentPos = mediaPlayer.getCurrentPosition();
                        seekBar.setProgress(currentPos);
                        txtCurrentTime.setText(formatTime(currentPos));
                        sendUpdateToMain();
                        handler.postDelayed(this, 1000);
                    } catch (Exception e) {
                        Log.e(TAG, "Update error: " + e.getMessage());
                    }
                }
            }
        };
        handler.post(updateSeekBar);
    }

    private String formatTime(int millis) {
        // (Code giữ nguyên)
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void startDiscAnimation() {
        // (Code giữ nguyên)
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
        // (Code giữ nguyên)
        if (imgCover != null) {
            imgCover.clearAnimation();
        }
    }

    // ========== BROADCAST COMMUNICATION ==========

    private void sendUpdateToMain() {
        if (playlist == null || currentSongIndex >= playlist.size()) return;

        Song song = playlist.get(currentSongIndex);
        int progress = (mediaPlayer != null && isPlaying) ? mediaPlayer.getCurrentPosition() : 0;
        int max = (mediaPlayer != null) ? mediaPlayer.getDuration() : 0;

        Intent intent = new Intent("PLAYER_UPDATE");
        intent.putExtra("action", "UPDATE_UI");
        intent.putExtra("title", song.title);
        intent.putExtra("artist", song.artist);
        intent.putExtra("cover", song.cover);
        intent.putExtra("isPlaying", isPlaying);
        intent.putExtra("progress", progress);
        intent.putExtra("max", max);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // ----- THAY ĐỔI 1: Cập nhật BroadcastReceiver này -----
    private BroadcastReceiver controlReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getStringExtra("action");
            if (action == null) return;

            // Dùng switch-case cho rõ ràng
            switch (action) {
                case "TOGGLE_PLAY_PAUSE":
                    togglePlayPause();
                    break;
                case "NEXT":
                    playNext();
                    break;
                case "PREVIOUS":
                    playPrevious();
                    break;
                case "STOP":
                    stopMusic();
                    break;
                case "REQUEST_UPDATE":
                    sendUpdateToMain();
                    break;

                // ----- THÊM 2 CASE NÀY VÀO -----
                case "ADD_TO_QUEUE": {
                    // Lấy bài hát từ intent
                    Song newSong = extractSongFromIntent(intent);
                    if (newSong != null && playlist != null) {
                        playlist.add(newSong); // Thêm vào cuối danh sách
                        Toast.makeText(context, "Đã thêm '" + newSong.title + "' vào hàng đợi", Toast.LENGTH_SHORT).show();
                    }
                    break;
                }

                case "PLAY_NEXT": {
                    // Lấy bài hát từ intent
                    Song newSong = extractSongFromIntent(intent);
                    if (newSong != null && playlist != null && playlist.size() > 0) {
                        // Thêm vào ngay sau bài hiện tại
                        playlist.add(currentSongIndex + 1, newSong);
                        Toast.makeText(context, "Phát tiếp theo: '" + newSong.title + "'", Toast.LENGTH_SHORT).show();
                    } else if (newSong != null && playlist != null) {
                        // Nếu playlist rỗng, chỉ cần thêm vào
                        playlist.add(newSong);
                    }
                    break;
                }
            }
        }
    };

    // ----- THAY ĐỔI 2: THÊM HÀM HELPER NÀY VÀO -----
    /**
     * Trích xuất thông tin bài hát từ Intent broadcast
     */
    private Song extractSongFromIntent(Intent intent) {
        String title = intent.getStringExtra("title");
        String artist = intent.getStringExtra("artist");
        String cover = intent.getStringExtra("cover");
        String preview = intent.getStringExtra("preview");

        if (title != null && artist != null && cover != null && preview != null) {
            // Dùng constructor của lớp Song lồng nhau (nested static class)
            return new Song(title, artist, cover, preview);
        }
        return null;
    }

    private void registerControlReceiver() {
        IntentFilter filter = new IntentFilter("PLAYER_CONTROL");
        LocalBroadcastManager.getInstance(this).registerReceiver(controlReceiver, filter);
    }

    private void stopMusic() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
            } catch (Exception e) {
                Log.e(TAG, "Stop error: " + e.getMessage());
            }
        }

        isPlaying = false;

        Intent intent = new Intent("PLAYER_UPDATE");
        intent.putExtra("action", "PLAYER_STOPPED");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        if (handler != null) {
            handler.removeCallbacks(updateSeekBar);
        }

        finish(); // Đóng hẳn activity khi bấm close
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(controlReceiver);

        // Khi activity bị hủy (không phải do thu nhỏ)
        if (isFinishing()) {
            if (mediaPlayer != null) {
                try {
                    if(mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                    }
                    mediaPlayer.release();
                    mediaPlayer = null;

                    // Thông báo cho MainActivity là đã dừng hẳn
                    Intent intent = new Intent("PLAYER_UPDATE");
                    intent.putExtra("action", "PLAYER_STOPPED");
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Release error: " + e.getMessage());
                }
            }
        }

        if (handler != null) {
            handler.removeCallbacks(updateSeekBar);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // (Code giữ nguyên)
        sendUpdateToMain();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // (Code giữ nguyên)
        sendUpdateToMain();
        if (mediaPlayer != null && isPlaying) {
            startSeekBarUpdater();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // (Code giữ nguyên)
        sendUpdateToMain();
    }

    @Override
    public void onBackPressed() {
        // (Code giữ nguyên - Thu nhỏ app)
        super.onBackPressed();
        sendUpdateToMain();
        moveTaskToBack(true); // Dùng true để nó giống nút Home
    }
}