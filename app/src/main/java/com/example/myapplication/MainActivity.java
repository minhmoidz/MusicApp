package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.libary.LibraryActivity;
import com.example.myapplication.playlist.PlaylistsActivity;
import com.example.myapplication.profile.AboutActivity;
import com.example.myapplication.profile.FavoritesActivity;
import com.example.myapplication.profile.ProfileActivity;
import com.example.myapplication.profile.HistoryActivity;
import com.example.myapplication.profile.SettingsActivity;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";

    // DrawerLayout
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    // Main content
    private RecyclerView recyclerView;
    private ArrayList<Song> songList;
    private ArrayList<Song> allSongs; // Lưu tất cả bài hát để lọc
    private MusicAdapter adapter;

    // Search bar
    private EditText etSearchBar;
    private boolean isSearching = false;

    // Mini Player views
    private View miniPlayer;
    private TextView tvMiniPlayerTitle;
    private TextView tvMiniPlayerArtist;
    private TextView btnMiniPlayerPlay;
    private TextView btnMiniPlayerNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupDrawer();
        setupRecyclerView();
        setupTopBar();
        setupSearchBar(); // Thêm setup cho thanh tìm kiếm lớn
        setupTabs();
        loadSongsFromApi();
    }

    private void setupDrawer() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);

        navigationView.setNavigationItemSelectedListener(this);

        // Setup menu button to open drawer
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

        // Setup header views
        View headerView = navigationView.getHeaderView(0);
        TextView tvUserName = headerView.findViewById(R.id.tvUserName);
        TextView tvUserEmail = headerView.findViewById(R.id.tvUserEmail);

        tvUserName.setText("Music Lover");
        tvUserEmail.setText("musiclover@zingmp3.vn");
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        songList = new ArrayList<>();
        allSongs = new ArrayList<>();
        adapter = new MusicAdapter(this, songList);
        recyclerView.setAdapter(adapter);
    }

    private void setupSearchBar() {
        etSearchBar = findViewById(R.id.etSearch);

        if (etSearchBar != null) {
            etSearchBar.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String query = s.toString().trim();

                    if (query.isEmpty()) {
                        // Nếu xóa hết text, hiển thị lại tất cả bài hát
                        isSearching = false;
                        songList.clear();
                        songList.addAll(allSongs);
                        adapter.notifyDataSetChanged();
                    } else if (query.length() >= 2) {
                        // Tìm kiếm khi nhập ít nhất 2 ký tự
                        isSearching = true;
                        searchSongsFromApi(query);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });

            // Khi focus vào thanh search
            etSearchBar.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    etSearchBar.setHint("Nhập tên bài hát, nghệ sĩ...");
                } else {
                    etSearchBar.setHint("Tìm kiếm...");
                }
            });
        }
    }

    private void setupTopBar() {
        TextView btnSearch = findViewById(R.id.btnSearch);
        TextView btnProfile = findViewById(R.id.btnProfile);

        if (btnSearch != null) {
            btnSearch.setOnClickListener(v -> {
                // Focus vào thanh tìm kiếm lớn
                if (etSearchBar != null) {
                    etSearchBar.requestFocus();
                    // Hiện bàn phím
                    etSearchBar.postDelayed(() -> {
                        android.view.inputmethod.InputMethodManager imm =
                                (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(etSearchBar, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                    }, 100);
                }
            });
        }

        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> {
                Intent intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
            });
        }

        TextView tvSeeAll = findViewById(R.id.tvSeeAll);
        if (tvSeeAll != null) {
            tvSeeAll.setOnClickListener(v ->
                    Toast.makeText(this, "See all songs", Toast.LENGTH_SHORT).show()
            );
        }
    }

    private void setupTabs() {
        TextView tabAll = findViewById(R.id.tabAll);
        TextView tabVPop = findViewById(R.id.tabVPop);
        TextView tabKPop = findViewById(R.id.tabKPop);
        TextView tabUSUK = findViewById(R.id.tabUSUK);

        // Tab All - Hiển thị bài hát hot
        if (tabAll != null) {
            tabAll.setOnClickListener(v -> {
                resetAllTabs(tabAll, tabVPop, tabKPop, tabUSUK);
                tabAll.setTextColor(0xFFFFFFFF);
                tabAll.setBackgroundResource(R.drawable.tab_selected);

                clearSearchBar();
                loadSongsFromApi();
                Toast.makeText(this, "Tất cả bài hát hot", Toast.LENGTH_SHORT).show();
            });
        }

        // Tab V-Pop
        if (tabVPop != null) {
            tabVPop.setOnClickListener(v -> {
                resetAllTabs(tabAll, tabVPop, tabKPop, tabUSUK);
                tabVPop.setTextColor(0xFFFFFFFF);
                tabVPop.setBackgroundResource(R.drawable.tab_selected);

                clearSearchBar();
                filterByGenre("vpop", "Sơn Tùng MTP Hòa Minzy Đen Vâu");
                Toast.makeText(this, "V-Pop", Toast.LENGTH_SHORT).show();
            });
        }

        // Tab K-Pop
        if (tabKPop != null) {
            tabKPop.setOnClickListener(v -> {
                resetAllTabs(tabAll, tabVPop, tabKPop, tabUSUK);
                tabKPop.setTextColor(0xFFFFFFFF);
                tabKPop.setBackgroundResource(R.drawable.tab_selected);

                clearSearchBar();
                filterByGenre("kpop", "BTS Blackpink Twice NewJeans");
                Toast.makeText(this, "K-Pop", Toast.LENGTH_SHORT).show();
            });
        }

        // Tab US-UK
        if (tabUSUK != null) {
            tabUSUK.setOnClickListener(v -> {
                resetAllTabs(tabAll, tabVPop, tabKPop, tabUSUK);
                tabUSUK.setTextColor(0xFFFFFFFF);
                tabUSUK.setBackgroundResource(R.drawable.tab_selected);

                clearSearchBar();
                filterByGenre("usuk", "Taylor Swift Ed Sheeran Ariana Grande");
                Toast.makeText(this, "US-UK", Toast.LENGTH_SHORT).show();
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

    private void filterByGenre(String genre, String searchQuery) {
        Log.d(TAG, "Filtering by genre: " + genre);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.deezer.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        DeezerApi api = retrofit.create(DeezerApi.class);

        api.searchTrack(searchQuery).enqueue(new Callback<TrackResponse>() {
            @Override
            public void onResponse(Call<TrackResponse> call, Response<TrackResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Genre filter results: " + response.body().data.size());

                    songList.clear();
                    allSongs.clear();

                    if (response.body().data.isEmpty()) {
                        Toast.makeText(MainActivity.this,
                                "Không tìm thấy bài hát " + genre.toUpperCase(),
                                Toast.LENGTH_SHORT).show();
                    } else {
                        for (TrackResponse.Track t : response.body().data) {
                            Song song = new Song(
                                    t.title,
                                    t.artist.name,
                                    t.album.cover,
                                    t.preview,
                                    ""
                            );
                            songList.add(song);
                            allSongs.add(song);
                        }

                        Toast.makeText(MainActivity.this,
                                "Đã tải " + songList.size() + " bài hát " + genre.toUpperCase(),
                                Toast.LENGTH_SHORT).show();
                    }

                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<TrackResponse> call, Throwable t) {
                Log.e(TAG, "Genre filter error: " + t.getMessage());
                Toast.makeText(MainActivity.this,
                        "Lỗi tải nhạc: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resetTab(TextView tab) {
        if (tab != null) {
            tab.setTextColor(0xFFAAAAAA);
            tab.setBackgroundResource(R.drawable.tab_unselected);
        }
    }

    private void searchSongsFromApi(String query) {
        Log.d(TAG, "Searching for: " + query);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.deezer.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        DeezerApi api = retrofit.create(DeezerApi.class);

        api.searchTrack(query).enqueue(new Callback<TrackResponse>() {
            @Override
            public void onResponse(Call<TrackResponse> call, Response<TrackResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Search results: " + response.body().data.size());

                    songList.clear();

                    if (response.body().data.isEmpty()) {
                        Toast.makeText(MainActivity.this,
                                "Không tìm thấy kết quả cho \"" + query + "\"",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        for (TrackResponse.Track t : response.body().data) {
                            Song song = new Song(
                                    t.title,
                                    t.artist.name,
                                    t.album.cover,
                                    t.preview,
                                    ""
                            );
                            songList.add(song);
                        }

                        // Chỉ cập nhật allSongs nếu không đang tìm kiếm từ search bar
                        if (!isSearching) {
                            allSongs.clear();
                            allSongs.addAll(songList);
                        }
                    }

                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<TrackResponse> call, Throwable t) {
                Log.e(TAG, "Search error: " + t.getMessage());
                Toast.makeText(MainActivity.this,
                        "Lỗi: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadSongsFromApi() {
        Log.d(TAG, "Loading songs from Deezer API...");

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.deezer.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        DeezerApi api = retrofit.create(DeezerApi.class);

        api.getChart().enqueue(new Callback<TrackResponse>() {
            @Override
            public void onResponse(Call<TrackResponse> call, Response<TrackResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "API Response successful! Songs: " + response.body().data.size());

                    songList.clear();
                    allSongs.clear();

                    for (TrackResponse.Track t : response.body().data) {
                        Song song = new Song(
                                t.title,
                                t.artist.name,
                                t.album.cover,
                                t.preview,
                                ""
                        );
                        songList.add(song);
                        allSongs.add(song);
                    }

                    adapter.notifyDataSetChanged();

                    if (miniPlayer != null && !songList.isEmpty()) {
                        miniPlayer.setVisibility(View.VISIBLE);
                        if (tvMiniPlayerTitle != null) {
                            tvMiniPlayerTitle.setText(songList.get(0).title);
                        }
                        if (tvMiniPlayerArtist != null) {
                            tvMiniPlayerArtist.setText(songList.get(0).artist);
                        }
                    }

                    Toast.makeText(MainActivity.this,
                            "Loaded " + songList.size() + " songs!",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<TrackResponse> call, Throwable t) {
                Log.e(TAG, "API error: " + t.getMessage());
                Toast.makeText(MainActivity.this,
                        "Error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            Toast.makeText(this, "Trang chủ", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_library) {
            Intent intent = new Intent(this, LibraryActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_favorites) {
            Intent intent = new Intent(this, FavoritesActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_playlists) {
            Intent intent = new Intent(this, PlaylistsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_history) {
            Intent intent = new Intent(this, HistoryActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_profile) {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_about) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_logout) {
            Toast.makeText(this, "Đăng xuất", Toast.LENGTH_SHORT).show();
            // TODO: Implement logout logic
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
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