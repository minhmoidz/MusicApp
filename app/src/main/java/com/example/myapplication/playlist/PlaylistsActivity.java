package com.example.myapplication.playlist;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;

import java.util.ArrayList;

public class PlaylistsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ArrayList<Playlist> playlists;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlists);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        playlists = new ArrayList<>();
        // TODO: Load playlists from database

        Toast.makeText(this, "Playlists feature coming soon!", Toast.LENGTH_SHORT).show();
    }
}

