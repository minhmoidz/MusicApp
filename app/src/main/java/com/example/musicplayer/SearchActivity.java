package com.example.musicplayer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.api.SpotifyApi;
import com.example.musicplayer.api.SpotifySearchResponse;
import com.example.musicplayer.api.SpotifyTrack;
import com.example.musicplayer.login.LoginActivity;
import com.example.musicplayer.playlist.PlayerActivity;
import com.google.gson.JsonObject;

import java.util.ArrayList;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SearchActivity extends AppCompatActivity implements MusicAdapter.OnItemClickListener {

    private static final String TAG = "SearchActivity";
    private static final String API_BASE_URL = "http://192.168.30.28:5030/";
    private static final long SEARCH_DELAY = 500; // 500ms

    private EditText etSearch;
    private TextView btnBack;
    private TextView tvNoResults;
    private RecyclerView recyclerViewSearch;
    private ArrayList<Song> searchResults;
    private MusicAdapter searchAdapter;

    private SpotifyApi spotifyApi;
    private LoginActivity.SessionManager sessionManager;
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        sessionManager = new LoginActivity.SessionManager(this);

        setupRetrofit();
        initViews();
        setupRecyclerView();
        setupSearchListener();
    }
    
    private void setupRetrofit() {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    String token = sessionManager.getAccessToken();
                    okhttp3.Request.Builder builder = chain.request().newBuilder();
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

    private void initViews() {
        etSearch = findViewById(R.id.etSearch);
        btnBack = findViewById(R.id.btnBack);
        tvNoResults = findViewById(R.id.tvNoResults);
        recyclerViewSearch = findViewById(R.id.recyclerViewSearch);

        etSearch.requestFocus();
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void setupRecyclerView() {
        recyclerViewSearch.setLayoutManager(new LinearLayoutManager(this));
        searchResults = new ArrayList<>();
        searchAdapter = new MusicAdapter(this, searchResults);
        searchAdapter.setOnItemClickListener(this);
        recyclerViewSearch.setAdapter(searchAdapter);
    }

    private void setupSearchListener() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                searchHandler.removeCallbacks(searchRunnable);

                if (query.length() >= 2) {
                    searchRunnable = () -> searchSongs(query);
                    searchHandler.postDelayed(searchRunnable, SEARCH_DELAY);
                } else {
                    searchResults.clear();
                    searchAdapter.notifyDataSetChanged();
                    tvNoResults.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void searchSongs(String query) {
        Log.d(TAG, "Searching Spotify for: " + query);

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("query", query);
        requestBody.addProperty("limit", 20);

        spotifyApi.searchTracks(requestBody).enqueue(new Callback<SpotifySearchResponse>() {
            @Override
            public void onResponse(@NonNull Call<SpotifySearchResponse> call, @NonNull Response<SpotifySearchResponse> response) {
                searchResults.clear();
                if (response.isSuccessful() && response.body() != null && response.body().tracks != null) {
                    if (response.body().tracks.isEmpty()) {
                        tvNoResults.setVisibility(View.VISIBLE);
                        tvNoResults.setText("Không tìm thấy kết quả cho \"" + query + "\"");
                    } else {
                        tvNoResults.setVisibility(View.GONE);
                        for (SpotifyTrack track : response.body().tracks) {
                            // FIX: Use the correct 10-argument constructor
                            Song song = new Song(
                                    track.id,
                                    track.name,
                                    track.getArtistsString(),
                                    track.imageUrl,
                                    track.previewUrl,
                                    "", // Lyrics
                                    track.album,
                                    track.durationMs,
                                    track.popularity,
                                    track.spotifyUrl
                            );
                            searchResults.add(song);
                        }
                    }
                } else {
                    tvNoResults.setVisibility(View.VISIBLE);
                    tvNoResults.setText("Lỗi tìm kiếm");
                }
                searchAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(@NonNull Call<SpotifySearchResponse> call, @NonNull Throwable t) {
                Toast.makeText(SearchActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                tvNoResults.setVisibility(View.VISIBLE);
                tvNoResults.setText("Không thể kết nối");
            }
        });
    }
    
    @Override
    public void onItemClick(String trackId) {
        // This logic now correctly calls the detail API
        spotifyApi.getTrackDetails(trackId).enqueue(new Callback<SpotifyTrack>() {
            @Override
            public void onResponse(@NonNull Call<SpotifyTrack> call, @NonNull Response<SpotifyTrack> response) {
                if (response.isSuccessful() && response.body() != null) {
                    SpotifyTrack track = response.body();

                    ArrayList<String> playlistTitles = new ArrayList<>();
                    ArrayList<String> playlistArtists = new ArrayList<>();
                    ArrayList<String> playlistCovers = new ArrayList<>();
                    ArrayList<String> playlistPreviews = new ArrayList<>();

                    playlistTitles.add(track.name);
                    playlistArtists.add(track.getArtistsString());
                    playlistCovers.add(track.imageUrl);
                    playlistPreviews.add(track.previewUrl != null ? track.previewUrl : "");

                    Intent intent = new Intent(SearchActivity.this, PlayerActivity.class);
                    intent.putStringArrayListExtra("playlist_titles", playlistTitles);
                    intent.putStringArrayListExtra("playlist_artists", playlistArtists);
                    intent.putStringArrayListExtra("playlist_covers", playlistCovers);
                    intent.putStringArrayListExtra("playlist_previews", playlistPreviews);
                    intent.putExtra("current_index", 0);
                    startActivity(intent);
                } else {
                    Toast.makeText(SearchActivity.this, "Could not load track details", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<SpotifyTrack> call, @NonNull Throwable t) {
                Toast.makeText(SearchActivity.this, "API Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
