package com.example.musicplayer.libary;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.musicplayer.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class LibraryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tabSongs, tabAlbums, tabArtists, tabFavorites;

    private ImageButton btnBackLibrary;
    private EditText searchBox;
    private LibraryAdapter adapter;
    private RequestQueue requestQueue;
    private List<LibraryItem> items = new ArrayList<>();
    private List<LibraryItem> allItems = new ArrayList<>();
    private FavoritesManager favoritesManager;

    private static final String SONGS_URL = "https://api.deezer.com/chart/0/tracks";
    private static final String ALBUMS_URL = "https://api.deezer.com/chart/0/albums";
    private static final String ARTISTS_URL = "https://api.deezer.com/chart/0/artists";
    private static final String SEARCH_URL = "https://api.deezer.com/search?q=";

    private enum TabType { SONGS, ALBUMS, ARTISTS, FAVORITES }
    private TabType currentTab = TabType.SONGS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        favoritesManager = new FavoritesManager(this);
        initViews();
        setupRecyclerView();
        setupTabs();
        setupBackButton();
        setupSearch();

        requestQueue = Volley.newRequestQueue(this);
        loadSongs();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        tabSongs = findViewById(R.id.tabSongs);
        tabAlbums = findViewById(R.id.tabAlbums);
        tabArtists = findViewById(R.id.tabArtists); // sẽ là null nếu layout không có
        tabFavorites = findViewById(R.id.tabFavorites);
        btnBackLibrary = findViewById(R.id.btnBackLibrary);
        searchBox = findViewById(R.id.searchBox);
    }


    private void setupRecyclerView() {
        adapter = new LibraryAdapter(this, items, new LibraryAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(LibraryItem item) {
                Intent intent = new Intent(LibraryActivity.this, com.example.musicplayer.playlist.PlayerActivity.class);

                // Truyền dữ liệu bài hát sang PlayerActivity
                intent.putExtra("title", item.getTitle());
                intent.putExtra("artist", item.getSubtitle());
                intent.putExtra("cover", item.getImageUrl());
                intent.putExtra("preview", item.getPreviewUrl());

                // Gửi kèm danh sách phát (playlist)
                ArrayList<String> titles = new ArrayList<>();
                ArrayList<String> artists = new ArrayList<>();
                ArrayList<String> covers = new ArrayList<>();
                ArrayList<String> previews = new ArrayList<>();

                for (LibraryItem song : items) {
                    titles.add(song.getTitle());
                    artists.add(song.getSubtitle());
                    covers.add(song.getImageUrl());
                    previews.add(song.getPreviewUrl());
                }

                intent.putStringArrayListExtra("playlist_titles", titles);
                intent.putStringArrayListExtra("playlist_artists", artists);
                intent.putStringArrayListExtra("playlist_covers", covers);
                intent.putStringArrayListExtra("playlist_previews", previews);
                intent.putExtra("current_index", items.indexOf(item));

                startActivity(intent);
            }


            @Override
            public void onFavoriteClick(LibraryItem item) {
                if (favoritesManager.isFavorite(item.getId())) {
                    favoritesManager.removeFavorite(item.getId());
                    Toast.makeText(LibraryActivity.this, "Đã xóa khỏi yêu thích", Toast.LENGTH_SHORT).show();
                } else {
                    favoritesManager.addFavorite(item);
                    Toast.makeText(LibraryActivity.this, "Đã thêm vào yêu thích", Toast.LENGTH_SHORT).show();
                }
                adapter.notifyDataSetChanged();
            }
        }, favoritesManager);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupTabs() {
        if (tabSongs != null) {
            tabSongs.setOnClickListener(v -> {
                if (currentTab != TabType.SONGS) {
                    currentTab = TabType.SONGS;
                    updateTabUI();
                    searchBox.setText("");
                    loadSongs();
                }
            });
        } else {
            android.util.Log.w("LibraryActivity", "tabSongs is null in setupTabs()");
        }

        if (tabAlbums != null) {
            tabAlbums.setOnClickListener(v -> {
                if (currentTab != TabType.ALBUMS) {
                    currentTab = TabType.ALBUMS;
                    updateTabUI();
                    searchBox.setText("");
                    loadAlbums();
                }
            });
        } else {
            android.util.Log.w("LibraryActivity", "tabAlbums is null in setupTabs()");
        }

        if (tabArtists != null) {
            tabArtists.setOnClickListener(v -> {
                if (currentTab != TabType.ARTISTS) {
                    currentTab = TabType.ARTISTS;
                    updateTabUI();
                    searchBox.setText("");
                    loadArtists();
                }
            });
        } else {
            android.util.Log.w("LibraryActivity", "tabArtists is null in setupTabs()");
        }

        if (tabFavorites != null) {
            tabFavorites.setOnClickListener(v -> {
                if (currentTab != TabType.FAVORITES) {
                    currentTab = TabType.FAVORITES;
                    updateTabUI();
                    searchBox.setText("");
                    loadFavorites();
                }
            });
        } else {
            android.util.Log.w("LibraryActivity", "tabFavorites is null in setupTabs()");
        }
    }

    private void setupBackButton() {
        btnBackLibrary.setOnClickListener(v -> finish());
    }

    private void setupSearch() {
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    filterItems(s.toString());
                } else {
                    reloadCurrentTab();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterItems(String query) {
        items.clear();
        String lowerQuery = query.toLowerCase();

        for (LibraryItem item : allItems) {
            if (item.getTitle().toLowerCase().contains(lowerQuery) ||
                    item.getSubtitle().toLowerCase().contains(lowerQuery)) {
                items.add(item);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void reloadCurrentTab() {
        switch (currentTab) {
            case SONGS:
                loadSongs();
                break;
            case ALBUMS:
                loadAlbums();
                break;
            case ARTISTS:
                loadArtists();
                break;
            case FAVORITES:
                loadFavorites();
                break;
        }
    }

    private void updateTabUI() {
        // Reset tất cả tabs
        tabSongs.setBackgroundResource(R.drawable.tab_unselected);
        tabSongs.setTextColor(0xFFAAAAAA);
        tabAlbums.setBackgroundResource(R.drawable.tab_unselected);
        tabAlbums.setTextColor(0xFFAAAAAA);
        tabArtists.setBackgroundResource(R.drawable.tab_unselected);
        tabArtists.setTextColor(0xFFAAAAAA);
        tabFavorites.setBackgroundResource(R.drawable.tab_unselected);
        tabFavorites.setTextColor(0xFFAAAAAA);

        // Highlight tab được chọn
        switch (currentTab) {
            case SONGS:
                tabSongs.setBackgroundResource(R.drawable.tab_selected);
                tabSongs.setTextColor(0xFFFFFFFF);
                break;
            case ALBUMS:
                tabAlbums.setBackgroundResource(R.drawable.tab_selected);
                tabAlbums.setTextColor(0xFFFFFFFF);
                break;
            case ARTISTS:
                tabArtists.setBackgroundResource(R.drawable.tab_selected);
                tabArtists.setTextColor(0xFFFFFFFF);
                break;
            case FAVORITES:
                tabFavorites.setBackgroundResource(R.drawable.tab_selected);
                tabFavorites.setTextColor(0xFFFFFFFF);
                break;
        }
    }

    private void loadSongs() {
        items.clear();
        allItems.clear();
        adapter.notifyDataSetChanged();

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, SONGS_URL, null,
                response -> {
                    try {
                        JSONArray data = response.getJSONArray("data");
                        for (int i = 0; i < Math.min(data.length(), 20); i++) {
                            JSONObject song = data.getJSONObject(i);
                            JSONObject artist = song.getJSONObject("artist");
                            JSONObject album = song.getJSONObject("album");

                            LibraryItem item = new LibraryItem(
                                    String.valueOf(song.getInt("id")),
                                    song.getString("title"),
                                    artist.getString("name"),
                                    album.getString("cover_medium"),
                                    song.getString("preview"),
                                    LibraryItem.Type.SONG
                            );
                            items.add(item);
                            allItems.add(item);
                        }
                        adapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        Toast.makeText(this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
        );

        requestQueue.add(request);
    }

    private void loadAlbums() {
        items.clear();
        allItems.clear();
        adapter.notifyDataSetChanged();

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, ALBUMS_URL, null,
                response -> {
                    try {
                        JSONArray data = response.getJSONArray("data");
                        for (int i = 0; i < Math.min(data.length(), 20); i++) {
                            JSONObject album = data.getJSONObject(i);
                            JSONObject artist = album.getJSONObject("artist");

                            LibraryItem item = new LibraryItem(
                                    String.valueOf(album.getInt("id")),
                                    album.getString("title"),
                                    artist.getString("name"),
                                    album.getString("cover_medium"),
                                    "",
                                    LibraryItem.Type.ALBUM
                            );
                            items.add(item);
                            allItems.add(item);
                        }
                        adapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        Toast.makeText(this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
        );

        requestQueue.add(request);
    }

    private void loadArtists() {
        items.clear();
        allItems.clear();
        adapter.notifyDataSetChanged();

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, ARTISTS_URL, null,
                response -> {
                    try {
                        JSONArray data = response.getJSONArray("data");
                        for (int i = 0; i < Math.min(data.length(), 20); i++) {
                            JSONObject artist = data.getJSONObject(i);

                            LibraryItem item = new LibraryItem(
                                    String.valueOf(artist.getInt("id")),
                                    artist.getString("name"),
                                    artist.getInt("nb_fan") + " fans",
                                    artist.getString("picture_medium"),
                                    "",
                                    LibraryItem.Type.ARTIST
                            );
                            items.add(item);
                            allItems.add(item);
                        }
                        adapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        Toast.makeText(this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
        );

        requestQueue.add(request);
    }

    private void loadFavorites() {
        items.clear();
        allItems.clear();

        List<LibraryItem> favorites = favoritesManager.getAllFavorites();
        items.addAll(favorites);
        allItems.addAll(favorites);

        adapter.notifyDataSetChanged();

        if (items.isEmpty()) {
            Toast.makeText(this, "Chưa có mục yêu thích nào", Toast.LENGTH_SHORT).show();
        }
    }
}