package com.example.musicplayer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.api.SpotifyApi;
import com.example.musicplayer.api.SpotifySearchResponse;
import com.example.musicplayer.api.SpotifyTrack;
import com.example.musicplayer.chatbot.ChatbotActivity;
import com.example.musicplayer.libary.LibraryActivity;
import com.example.musicplayer.login.LoginActivity;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, MusicAdapter.OnItemClickListener {

    private static final String TAG = "MainActivity";
    private static final String API_BASE_URL = "http://192.168.30.28:5030/";
    private static final long SEARCH_DELAY = 500; // 500ms delay for search

    // UI Components
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private RecyclerView recyclerView;
    private EditText etSearchBar;

    // Data
    private ArrayList<Song> songList;
    private ArrayList<Song> allSongs;
    private MusicAdapter adapter;
    private SpotifyApi spotifyApi;
    private LoginActivity.SessionManager sessionManager;
    private LoginActivity.AuthTokenResponse.User currentUser;

    // Search
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sessionManager = new LoginActivity.SessionManager(this);

        setupRetrofit();
        setupDrawer();
        setupRecyclerView();
        setupTopBar();
        setupSearchBar();
        setupTabs();
        setupChatbot();

        loadInitialData();
    }

    private void setupRetrofit() {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    String token = sessionManager.getAccessToken();
                    Request.Builder builder = chain.request().newBuilder();
                    if (token != null) {
                        builder.header("Authorization", "Bearer " + token);
                    }
                    return chain.proceed(builder.build());
                })
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        spotifyApi = retrofit.create(SpotifyApi.class);
    }

    private void loadInitialData() {
        loadUserProfile();
        loadRecommendedSongs();
    }

    private void loadUserProfile() {
        spotifyApi.getMe().enqueue(new Callback<LoginActivity.AuthTokenResponse.User>() {
            @Override
            public void onResponse(@NonNull Call<LoginActivity.AuthTokenResponse.User> call, @NonNull Response<LoginActivity.AuthTokenResponse.User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentUser = response.body();
                    updateDrawerHeader();
                } else {
                    handleLogout();
                }
            }

            @Override
            public void onFailure(@NonNull Call<LoginActivity.AuthTokenResponse.User> call, @NonNull Throwable t) {
                Toast.makeText(MainActivity.this, "Could not load user profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateDrawerHeader() {
        if (currentUser != null) {
            View headerView = navigationView.getHeaderView(0);
            TextView tvUserName = headerView.findViewById(R.id.tvUserName);
            TextView tvUserEmail = headerView.findViewById(R.id.tvUserEmail);
            tvUserName.setText(currentUser.fullName);
            tvUserEmail.setText(currentUser.email);
        }
    }

    private void loadRecommendedSongs() {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("seed_genres", "pop,rock,vietnamese");
        requestBody.addProperty("limit", 20);

        spotifyApi.getRecommendations(requestBody).enqueue(new Callback<List<SpotifyTrack>>() {
            @Override
            public void onResponse(@NonNull Call<List<SpotifyTrack>> call, @NonNull Response<List<SpotifyTrack>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateSongList(response.body());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<SpotifyTrack>> call, @NonNull Throwable t) {
                 Toast.makeText(MainActivity.this, "Failed to load recommendations", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchSongs(String query) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("query", query);
        requestBody.addProperty("limit", 20);

        spotifyApi.searchTracks(requestBody).enqueue(new Callback<SpotifySearchResponse>() {
            @Override
            public void onResponse(@NonNull Call<SpotifySearchResponse> call, @NonNull Response<SpotifySearchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateSongList(response.body().tracks);
                }
            }

            @Override
            public void onFailure(@NonNull Call<SpotifySearchResponse> call, @NonNull Throwable t) {
                Toast.makeText(MainActivity.this, "Search failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateSongList(List<SpotifyTrack> tracks) {
        songList.clear();
        for (SpotifyTrack track : tracks) {
            songList.add(new Song(track.id, track.name, track.getArtistsString(), track.imageUrl, track.previewUrl, "", track.album, track.durationMs, track.popularity, track.spotifyUrl));
        }
        allSongs.clear();
        allSongs.addAll(songList);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(String trackId) {
        ArrayList<String> playlistTitles = new ArrayList<>();
        ArrayList<String> playlistArtists = new ArrayList<>();
        ArrayList<String> playlistCovers = new ArrayList<>();
        ArrayList<String> playlistPreviews = new ArrayList<>();
        ArrayList<String> playlistIds = new ArrayList<>();
        ArrayList<Integer> playlistDurations = new ArrayList<>();
        int clickedIndex = -1;

        for (int i = 0; i < songList.size(); i++) {
            Song song = songList.get(i);
            playlistTitles.add(song.title);
            playlistArtists.add(song.artist);
            playlistCovers.add(song.cover);
            playlistPreviews.add(song.audio != null ? song.audio : "");
            playlistIds.add(song.id);
            playlistDurations.add(song.durationMs);

            if (song.id.equals(trackId)) {
                clickedIndex = i;
            }
        }

        if (clickedIndex != -1) {
            Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
            intent.putStringArrayListExtra("playlist_titles", playlistTitles);
            intent.putStringArrayListExtra("playlist_artists", playlistArtists);
            intent.putStringArrayListExtra("playlist_covers", playlistCovers);
            intent.putStringArrayListExtra("playlist_previews", playlistPreviews);
            intent.putStringArrayListExtra("playlist_ids", playlistIds);
            intent.putIntegerArrayListExtra("playlist_durations", playlistDurations);
            intent.putExtra("current_index", clickedIndex);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Could not find the clicked song.", Toast.LENGTH_SHORT).show();
        }
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
        findViewById(R.id.btnMenu).setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
    }

    private void setupTopBar() {
        findViewById(R.id.btnProfile).setOnClickListener(v -> openProfile());
    }

    private void openProfile() {
        if (currentUser != null) {
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.putExtra("USER_NAME", currentUser.fullName);
            intent.putExtra("USER_EMAIL", currentUser.email);
            startActivity(intent);
        } else {
            Toast.makeText(this, "User data not loaded yet", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleLogout() {
        sessionManager.clear();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_profile) {
            openProfile();
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.nav_logout) {
            handleLogout();
        } else if (id == R.id.nav_library) {
            startActivity(new Intent(this, LibraryActivity.class));
        } else if (id == R.id.nav_favorites) {
            startActivity(new Intent(this, FavoritesActivity.class));
        } else if (id == R.id.nav_playlists) {
            startActivity(new Intent(this, PlaylistsActivity.class));
        } else if (id == R.id.nav_history) {
            startActivity(new Intent(this, HistoryActivity.class));
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
    
    private void setupSearchBar() {
        etSearchBar = findViewById(R.id.etSearch);
        etSearchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                searchHandler.removeCallbacks(searchRunnable);

                if (query.isEmpty()) {
                    if (songList.size() != allSongs.size()) {
                        songList.clear();
                        songList.addAll(allSongs);
                        adapter.notifyDataSetChanged();
                    }
                } else {
                    searchRunnable = () -> searchSongs(query);
                    searchHandler.postDelayed(searchRunnable, SEARCH_DELAY);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
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
    }

    private void setupChatbot(){
        FloatingActionButton btnChatbot = findViewById(R.id.btnChatBot);
        btnChatbot.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatbotActivity.class);
            startActivity(intent);
        });
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
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
