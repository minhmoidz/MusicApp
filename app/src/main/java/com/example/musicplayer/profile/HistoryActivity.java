package com.example.musicplayer.profile;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.R;
import com.example.musicplayer.api.HistoryResponse;
import com.example.musicplayer.api.SpotifyApi;
import com.example.musicplayer.login.LoginActivity;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerViewHistory;
    private HistoryAdapter adapter;
    private List<HistoryResponse.HistoryItem> historyList = new ArrayList<>();
    private SpotifyApi spotifyApi;
    private LoginActivity.SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        sessionManager = new LoginActivity.SessionManager(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        setupRetrofit();
        setupRecyclerView();
        fetchHistory();
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
                .baseUrl("http://192.168.30.28:5030/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        spotifyApi = retrofit.create(SpotifyApi.class);
    }

    private void setupRecyclerView() {
        recyclerViewHistory = findViewById(R.id.recyclerViewHistory);
        recyclerViewHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(historyList);
        recyclerViewHistory.setAdapter(adapter);
    }

    private void fetchHistory() {
        // Pass null for trackId to get all history (not filtered)
        spotifyApi.getHistory(20, 0, null).enqueue(new Callback<HistoryResponse>() {
            @Override
            public void onResponse(@NonNull Call<HistoryResponse> call, @NonNull Response<HistoryResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    HistoryResponse historyResponse = response.body();
                    if (historyResponse.data != null && historyResponse.data.items != null) {
                        historyList.clear();
                        historyList.addAll(historyResponse.data.items);
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(HistoryActivity.this, "No history data available", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(HistoryActivity.this, "Failed to load history: " + response.code(),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<HistoryResponse> call, @NonNull Throwable t) {
                Toast.makeText(HistoryActivity.this, "History API call failed: " + t.getMessage(), Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }
}
