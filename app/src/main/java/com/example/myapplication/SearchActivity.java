package com.example.myapplication;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SearchActivity extends AppCompatActivity {

    private static final String TAG = "SearchActivity";

    private EditText etSearch;
    private TextView btnBack;
    private TextView tvNoResults;
    private RecyclerView recyclerViewSearch;
    private ArrayList<Song> searchResults;
    private MusicAdapter searchAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        initViews();
        setupRecyclerView();
        setupSearchListener();
    }

    private void initViews() {
        etSearch = findViewById(R.id.etSearch);
        btnBack = findViewById(R.id.btnBack);
        tvNoResults = findViewById(R.id.tvNoResults);
        recyclerViewSearch = findViewById(R.id.recyclerViewSearch);

        // Focus vào ô tìm kiếm khi mở activity
        etSearch.requestFocus();

        // Nút back
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void setupRecyclerView() {
        recyclerViewSearch.setLayoutManager(new LinearLayoutManager(this));
        searchResults = new ArrayList<>();
        searchAdapter = new MusicAdapter(this, searchResults);
        recyclerViewSearch.setAdapter(searchAdapter);
    }

    private void setupSearchListener() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.length() >= 2) {
                    searchSongs(query);
                } else if (query.isEmpty()) {
                    searchResults.clear();
                    searchAdapter.notifyDataSetChanged();
                    tvNoResults.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void searchSongs(String query) {
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

                    searchResults.clear();

                    if (response.body().data.isEmpty()) {
                        tvNoResults.setVisibility(View.VISIBLE);
                        tvNoResults.setText("Không tìm thấy kết quả cho \"" + query + "\"");
                    } else {
                        tvNoResults.setVisibility(View.GONE);
                        for (TrackResponse.Track t : response.body().data) {
                            Song song = new Song(
                                    t.title,
                                    t.artist.name,
                                    t.album.cover,
                                    t.preview,
                                    ""
                            );
                            searchResults.add(song);
                        }
                    }

                    searchAdapter.notifyDataSetChanged();
                } else {
                    Log.e(TAG, "Search failed: " + response.code());
                    tvNoResults.setVisibility(View.VISIBLE);
                    tvNoResults.setText("Lỗi tìm kiếm");
                }
            }

            @Override
            public void onFailure(Call<TrackResponse> call, Throwable t) {
                Log.e(TAG, "Search error: " + t.getMessage());
                Toast.makeText(SearchActivity.this,
                        "Lỗi: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
                tvNoResults.setVisibility(View.VISIBLE);
                tvNoResults.setText("Không thể kết nối");
            }
        });
    }
}