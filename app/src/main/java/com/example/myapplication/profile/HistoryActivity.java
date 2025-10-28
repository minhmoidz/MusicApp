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

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MusicAdapter adapter;
    private ArrayList<Song> historySongs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        historySongs = new ArrayList<>();
        // TODO: Load play history from database (with timestamp)

        adapter = new MusicAdapter(this, historySongs);
        recyclerView.setAdapter(adapter);

        if (historySongs.isEmpty()) {
            Toast.makeText(this, "Chưa có lịch sử phát", Toast.LENGTH_SHORT).show();
        }
    }
}
