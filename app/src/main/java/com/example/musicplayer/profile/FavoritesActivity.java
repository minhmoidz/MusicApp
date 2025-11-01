package com.example.musicplayer.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicplayer.R;
import com.example.musicplayer.playlist.PlayerActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

public class FavoritesActivity extends AppCompatActivity implements FavoritesManager.FavoritesChangeListener {

    private RecyclerView recyclerView;
    private FavoritesAdapter adapter;
    private ArrayList<FavoritesManager.FavoriteSong> favoriteSongs;
    private FavoritesManager favoritesManager;
    private TextView txtEmptyState, txtSongCount;
    private ImageButton btnBack, btnClearAll;
    private View emptyStateLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        favoritesManager = FavoritesManager.getInstance(this);
        favoritesManager.addListener(this);

        initViews();
        loadFavorites();
        setupRecyclerView();
        setupButtons();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerFavorites);
        txtEmptyState = findViewById(R.id.txtEmptyState);
        txtSongCount = findViewById(R.id.txtSongCount);
        btnBack = findViewById(R.id.btnBack);
        btnClearAll = findViewById(R.id.btnClearAll);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
    }

    private void loadFavorites() {
        favoriteSongs = favoritesManager.getFavorites();

        // Sắp xếp theo thời gian thêm mới nhất
        Collections.sort(favoriteSongs, (s1, s2) -> Long.compare(s2.timestamp, s1.timestamp));

        updateUI();
    }

    private void updateUI() {
        if (favoriteSongs.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
            btnClearAll.setVisibility(View.GONE);
            txtSongCount.setText("0 bài hát");
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
            btnClearAll.setVisibility(View.VISIBLE);
            txtSongCount.setText(favoriteSongs.size() + " bài hát");

            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FavoritesAdapter(favoriteSongs);
        recyclerView.setAdapter(adapter);

        // Swipe to delete
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                FavoritesManager.FavoriteSong song = favoriteSongs.get(position);

                // Xóa khỏi favorites
                favoritesManager.removeFavorite(song.title, song.artist);

                Toast.makeText(FavoritesActivity.this, "Đã xóa " + song.title, Toast.LENGTH_SHORT).show();
            }
        });
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void setupButtons() {
        btnBack.setOnClickListener(v -> finish());

        btnClearAll.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Xóa tất cả")
                    .setMessage("Bạn có chắc muốn xóa tất cả bài hát yêu thích?")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        favoritesManager.clearAllFavorites();
                        Toast.makeText(this, "Đã xóa tất cả", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });
    }

    @Override
    public void onFavoritesChanged() {
        runOnUiThread(() -> loadFavorites());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        favoritesManager.removeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFavorites();
    }

    // Adapter
    private class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.ViewHolder> {
        private ArrayList<FavoritesManager.FavoriteSong> songs;

        public FavoritesAdapter(ArrayList<FavoritesManager.FavoriteSong> songs) {
            this.songs = songs;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_favorite_song, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            FavoritesManager.FavoriteSong song = songs.get(position);

            holder.txtTitle.setText(song.title);
            holder.txtArtist.setText(song.artist);

            // Format timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            holder.txtDate.setText("Đã thêm: " + sdf.format(new Date(song.timestamp)));

            // Load cover image
            if (song.cover != null && !song.cover.isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(song.cover)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_gallery)
                        .centerCrop()
                        .into(holder.imgCover);
            } else {
                holder.imgCover.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            // Click to play
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(FavoritesActivity.this, PlayerActivity.class);
                intent.putExtra("title", song.title);
                intent.putExtra("artist", song.artist);
                intent.putExtra("cover", song.cover);
                intent.putExtra("preview", song.preview);

                // Tạo playlist từ danh sách yêu thích
                ArrayList<String> titles = new ArrayList<>();
                ArrayList<String> artists = new ArrayList<>();
                ArrayList<String> covers = new ArrayList<>();
                ArrayList<String> previews = new ArrayList<>();

                for (FavoritesManager.FavoriteSong s : songs) {
                    titles.add(s.title);
                    artists.add(s.artist);
                    covers.add(s.cover);
                    previews.add(s.preview);
                }

                intent.putStringArrayListExtra("playlist_titles", titles);
                intent.putStringArrayListExtra("playlist_artists", artists);
                intent.putStringArrayListExtra("playlist_covers", covers);
                intent.putStringArrayListExtra("playlist_previews", previews);
                intent.putExtra("current_index", position);

                startActivity(intent);
            });

            // Delete button
            holder.btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(FavoritesActivity.this)
                        .setTitle("Xóa bài hát")
                        .setMessage("Xóa \"" + song.title + "\" khỏi yêu thích?")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            favoritesManager.removeFavorite(song.title, song.artist);
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            });

            // Animation
            holder.itemView.setAlpha(0f);
            holder.itemView.setTranslationY(50f);
            holder.itemView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(300)
                    .setStartDelay(position * 50L)
                    .start();
        }

        @Override
        public int getItemCount() {
            return songs.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imgCover;
            TextView txtTitle, txtArtist, txtDate;
            ImageButton btnDelete;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                imgCover = itemView.findViewById(R.id.imgCover);
                txtTitle = itemView.findViewById(R.id.txtTitle);
                txtArtist = itemView.findViewById(R.id.txtArtist);
                txtDate = itemView.findViewById(R.id.txtDate);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }
        }
    }
}