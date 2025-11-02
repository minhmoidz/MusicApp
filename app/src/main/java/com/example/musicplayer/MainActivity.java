package com.example.musicplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicplayer.api.SpotifyApi;
import com.example.musicplayer.api.SpotifySearchResponse;
import com.example.musicplayer.api.SpotifyTrack;
import com.example.musicplayer.chatbot.ChatbotActivity;
import com.example.musicplayer.libary.LibraryActivity;
import com.example.musicplayer.playlist.PlayerActivity;
import com.example.musicplayer.playlist.PlaylistsActivity;
import com.example.musicplayer.profile.AboutActivity;
import com.example.musicplayer.profile.FavoritesActivity;
import com.example.musicplayer.profile.HistoryActivity;
import com.example.musicplayer.profile.ProfileActivity;
import com.example.musicplayer.profile.SettingsActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, MusicAdapter.OnItemClickListener {

    private static final String TAG = "MainActivity";
    private static final String API_BASE_URL = "http://192.168.30.28:5030/";
    private static final long SEARCH_DELAY = 500;

    // UI Components - Main
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private RecyclerView recyclerView;
    private EditText etSearchBar;

    // Mini Player UI
    private CardView miniPlayerCard;
    private ImageView miniImgCover;
    private TextView miniTxtTitle, miniTxtArtist;
    private ImageButton miniBtnPlayPause, miniBtnNext, miniBtnClose;
    private ProgressBar miniProgressBar;

    // Data
    private ArrayList<Song> songList;
    private ArrayList<Song> allSongs;
    private MusicAdapter adapter;
    private SpotifyApi spotifyApi;
    private boolean isSearching = false;

    // Search handler
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Handler updateHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private Runnable updateProgressRunnable;

    // Player state - LƯU TRẠNG THÁI
    private boolean isPlayerActive = false;
    private String currentTitle = "";
    private String currentArtist = "";
    private String currentCover = "";
    private boolean currentIsPlaying = false;
    private int currentProgress = 0;
    private int currentMax = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupRetrofit();
        setupDrawer();
        setupRecyclerView();
        setupTopBar();
        setupSearchBar();
        setupTabs();
        setupChatbot();
        setupMiniPlayer();
        loadRecommendedSongs();

        // Register broadcast receiver for player updates
        registerPlayerReceiver();
    }

    // ========== MINI PLAYER SETUP ==========

    private void setupMiniPlayer() {
        miniPlayerCard = findViewById(R.id.miniPlayerCard);
        miniImgCover = findViewById(R.id.miniImgCover);
        miniTxtTitle = findViewById(R.id.miniTxtTitle);
        miniTxtArtist = findViewById(R.id.miniTxtArtist);
        miniBtnPlayPause = findViewById(R.id.miniBtnPlayPause);
        miniBtnNext = findViewById(R.id.miniBtnNext);
        miniBtnClose = findViewById(R.id.miniBtnClose);
        miniProgressBar = findViewById(R.id.miniProgressBar);

        // Click mini player to open PlayerActivity
        miniPlayerCard.setOnClickListener(v -> {
            Intent intent = new Intent(this, PlayerActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });

        // Mini Play/Pause
        miniBtnPlayPause.setOnClickListener(v -> {
            sendBroadcastToPlayer("TOGGLE_PLAY_PAUSE");
        });

        // Mini Next
        miniBtnNext.setOnClickListener(v -> {
            sendBroadcastToPlayer("NEXT");
        });

        // Mini Close - Stop music and hide
        miniBtnClose.setOnClickListener(v -> {
            sendBroadcastToPlayer("STOP");
            hideMiniPlayer();
        });
    }

    private void showMiniPlayer(String title, String artist, String cover, boolean isPlaying) {
        isPlayerActive = true;
        miniPlayerCard.setVisibility(View.VISIBLE);

        miniTxtTitle.setText(title);
        miniTxtArtist.setText(artist);

        // Load cover image
        Glide.with(this)
                .load(cover)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .into(miniImgCover);

        // Update play/pause button
        int iconRes = isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
        miniBtnPlayPause.setImageResource(iconRes);

        // Animate entrance
        miniPlayerCard.setAlpha(0f);
        miniPlayerCard.setTranslationY(100f);
        miniPlayerCard.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .start();
    }

    private void updateMiniPlayer(boolean isPlaying, int progress, int max) {
        if (miniPlayerCard.getVisibility() == View.VISIBLE) {
            int iconRes = isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
            miniBtnPlayPause.setImageResource(iconRes);

            if (max > 0) {
                miniProgressBar.setMax(max);
                miniProgressBar.setProgress(progress);
            }
        }
    }

    private void hideMiniPlayer() {
        isPlayerActive = false;
        miniPlayerCard.animate()
                .alpha(0f)
                .translationY(100f)
                .setDuration(300)
                .withEndAction(() -> miniPlayerCard.setVisibility(View.GONE))
                .start();
    }

    // ========== BROADCAST COMMUNICATION WITH PLAYERACTIVITY ==========

    private void sendBroadcastToPlayer(String action) {
        Intent intent = new Intent("PLAYER_CONTROL");
        intent.putExtra("action", action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private BroadcastReceiver playerUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getStringExtra("action");

            if ("UPDATE_UI".equals(action)) {
                String title = intent.getStringExtra("title");
                String artist = intent.getStringExtra("artist");
                String cover = intent.getStringExtra("cover");
                boolean isPlaying = intent.getBooleanExtra("isPlaying", false);
                int progress = intent.getIntExtra("progress", 0);
                int max = intent.getIntExtra("max", 0);

                if (title != null && artist != null) {
                    showMiniPlayer(title, artist, cover, isPlaying);
                    updateMiniPlayer(isPlaying, progress, max);
                }
            } else if ("PLAYER_STOPPED".equals(action)) {
                hideMiniPlayer();
            }
        }
    };

    private void registerPlayerReceiver() {
        IntentFilter filter = new IntentFilter("PLAYER_UPDATE");
        LocalBroadcastManager.getInstance(this).registerReceiver(playerUpdateReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(playerUpdateReceiver);
        if (updateHandler != null) {
            updateHandler.removeCallbacks(updateProgressRunnable);
        }
    }

    // ========== EXISTING METHODS (unchanged) ==========

    private void setupRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        spotifyApi = retrofit.create(SpotifyApi.class);
    }

    private void loadRecommendedSongs() {
        Log.d(TAG, "Loading recommended songs from Spotify API...");

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("seed_genres", "pop,rock,vietnamese");
        requestBody.addProperty("limit", 20);

        spotifyApi.getRecommendations(requestBody).enqueue(new Callback<List<SpotifyTrack>>() {
            @Override
            public void onResponse(Call<List<SpotifyTrack>> call, Response<List<SpotifyTrack>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateSongList(response.body());
                } else {
                    Toast.makeText(MainActivity.this, "Error loading songs", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<SpotifyTrack>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Failure: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchSongs(String query) {
        Log.d(TAG, "Searching for: " + query);

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("query", query);
        requestBody.addProperty("limit", 20);

        spotifyApi.searchTracks(requestBody).enqueue(new Callback<SpotifySearchResponse>() {
            @Override
            public void onResponse(Call<SpotifySearchResponse> call, Response<SpotifySearchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateSongList(response.body().tracks);
                } else {
                    Toast.makeText(MainActivity.this, "Search failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SpotifySearchResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateSongList(List<SpotifyTrack> tracks) {
        songList.clear();
        for (SpotifyTrack track : tracks) {
            Song song = new Song(
                    track.id,
                    track.name,
                    track.getArtistsString(),
                    track.imageUrl,
                    track.previewUrl,
                    ""
            );
            songList.add(song);
        }

        if (!isSearching) {
            allSongs.clear();
            allSongs.addAll(songList);
        }

        adapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(String trackId) {
        Log.d(TAG, "Item clicked: " + trackId);
        spotifyApi.getTrackDetails(trackId).enqueue(new Callback<SpotifyTrack>() {
            @Override
            public void onResponse(Call<SpotifyTrack> call, Response<SpotifyTrack> response) {
                if (response.isSuccessful() && response.body() != null) {
                    SpotifyTrack track = response.body();

                    // Create playlist from current song list
                    ArrayList<String> playlistTitles = new ArrayList<>();
                    ArrayList<String> playlistArtists = new ArrayList<>();
                    ArrayList<String> playlistCovers = new ArrayList<>();
                    ArrayList<String> playlistPreviews = new ArrayList<>();

                    int clickedIndex = 0;
                    for (int i = 0; i < songList.size(); i++) {
                        Song song = songList.get(i);
                        playlistTitles.add(song.title);
                        playlistArtists.add(song.artist);
                        playlistCovers.add(song.cover);
                        playlistPreviews.add(song.audio);

                        if (song.id.equals(trackId)) {
                            clickedIndex = i;
                        }
                    }

                    Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
                    intent.putStringArrayListExtra("playlist_titles", playlistTitles);
                    intent.putStringArrayListExtra("playlist_artists", playlistArtists);
                    intent.putStringArrayListExtra("playlist_covers", playlistCovers);
                    intent.putStringArrayListExtra("playlist_previews", playlistPreviews);
                    intent.putExtra("current_index", clickedIndex);
                    startActivity(intent);
                }
            }

            @Override
            public void onFailure(Call<SpotifyTrack> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Could not load track details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        songList = new ArrayList<>();
        allSongs = new ArrayList<>();
        adapter = new MusicAdapter(this, songList);
        adapter.setOnItemClickListener(this);
        recyclerView.setAdapter(adapter);
    }

    private void setupDrawer() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(this);
        TextView btnMenu = findViewById(R.id.btnMenu);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }
        View headerView = navigationView.getHeaderView(0);
        TextView tvUserName = headerView.findViewById(R.id.tvUserName);
        TextView tvUserEmail = headerView.findViewById(R.id.tvUserEmail);
        tvUserName.setText("Music Lover");
        tvUserEmail.setText("musiclover@zingmp3.vn");
    }

    private void setupSearchBar() {
        etSearchBar = findViewById(R.id.etSearch);
        if (etSearchBar != null) {
            etSearchBar.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String query = s.toString().trim();
                    searchHandler.removeCallbacks(searchRunnable);

                    if (query.isEmpty()) {
                        isSearching = false;
                        songList.clear();
                        songList.addAll(allSongs);
                        adapter.notifyDataSetChanged();
                    } else {
                        isSearching = true;
                        searchRunnable = () -> searchSongs(query);
                        searchHandler.postDelayed(searchRunnable, SEARCH_DELAY);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void setupTopBar() {
        ImageView btnProfile = findViewById(R.id.btnProfile);
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> {
                Intent intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
            });
        }
    }

    private void setupTabs() {
        TextView tabAll = findViewById(R.id.tabAll);
        TextView tabVPop = findViewById(R.id.tabVPop);
        TextView tabKPop = findViewById(R.id.tabKPop);
        TextView tabUSUK = findViewById(R.id.tabUSUK);

        View.OnClickListener tabClickListener = v -> {
            resetAllTabs(tabAll, tabVPop, tabKPop, tabUSUK);
            TextView clickedTab = (TextView) v;
            clickedTab.setTextColor(0xFFFFFFFF);
            clickedTab.setBackgroundResource(R.drawable.tab_selected);
            clearSearchBar();
            isSearching = false;

            int id = v.getId();
            if (id == R.id.tabAll) {
                loadRecommendedSongs();
            } else if (id == R.id.tabVPop) {
                searchSongs("V-Pop");
            } else if (id == R.id.tabKPop) {
                searchSongs("K-Pop");
            } else if (id == R.id.tabUSUK) {
                searchSongs("US-UK");
            }
        };

        tabAll.setOnClickListener(tabClickListener);
        tabVPop.setOnClickListener(tabClickListener);
        tabKPop.setOnClickListener(tabClickListener);
        tabUSUK.setOnClickListener(tabClickListener);

        tabAll.setTextColor(0xFFFFFFFF);
        tabAll.setBackgroundResource(R.drawable.tab_selected);
    }

    private void setupChatbot(){
        FloatingActionButton btnChatbot = findViewById(R.id.btnChatBot);
        if (btnChatbot != null) {
            btnChatbot.setOnClickListener(v -> {
                Intent intent = new Intent(this, ChatbotActivity.class);
                startActivity(intent);
            });
        }
    }

    private void resetAllTabs(TextView... tabs) {
        for (TextView tab : tabs) {
            if (tab != null) {
                tab.setTextColor(0xFFAAAAAA);
                tab.setBackgroundResource(R.drawable.tab_unselected);
            }
        }
    }

    private void clearSearchBar() {
        if (etSearchBar != null) {
            etSearchBar.setText("");
            etSearchBar.clearFocus();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_home) {
            // Already home
        } else if (id == R.id.nav_library) {
            startActivity(new Intent(this, LibraryActivity.class));
        } else if (id == R.id.nav_favorites) {
            startActivity(new Intent(this, FavoritesActivity.class));
        } else if (id == R.id.nav_playlists) {
            startActivity(new Intent(this, PlaylistsActivity.class));
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Request update from PlayerActivity if it's running
        sendBroadcastToPlayer("REQUEST_UPDATE");
    }
}