package com.example.myapplication.profile;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.MusicAdapter;
import com.example.myapplication.R;
import com.example.myapplication.Song;

import java.util.ArrayList;

public class FavoritesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MusicAdapter adapter;
    private ArrayList<Song> favoriteSongs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        favoriteSongs = new ArrayList<>();
        // TODO: Load favorite songs from database/SharedPreferences

        adapter = new MusicAdapter(this, favoriteSongs);
        recyclerView.setAdapter(adapter);

        if (favoriteSongs.isEmpty()) {
            Toast.makeText(this, "Chưa có bài hát yêu thích", Toast.LENGTH_SHORT).show();
        }
    }
}