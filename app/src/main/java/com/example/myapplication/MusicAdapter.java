package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.myapplication.playlist.PlayerActivity;

import java.util.ArrayList;
import java.util.List;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.ViewHolder> {

    private static final String TAG = "MusicAdapter";
    private final Context context;
    private final List<Song> songs;

    public MusicAdapter(Context context, List<Song> songs) {
        this.context = context;
        this.songs = songs;
    }

    @NonNull
    @Override
    public MusicAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_song, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MusicAdapter.ViewHolder holder, int position) {
        Song song = songs.get(position);

        // Rank number with animation
        holder.tvRank.setText(String.valueOf(position + 1));

        // Color for top 3
        if (position < 3) {
            holder.tvRank.setTextColor(0xFFFFD700); // Gold
            holder.tvRank.setTextSize(20);
            holder.tvRank.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            holder.tvRank.setTextColor(0xFFB0B0B0); // Gray
            holder.tvRank.setTextSize(16);
            holder.tvRank.setTypeface(null, android.graphics.Typeface.NORMAL);
        }

        // Song title
        holder.tvSongTitle.setText(song.title != null ? song.title : "Unknown");

        // Artist
        holder.tvArtist.setText(song.artist != null ? song.artist : "Unknown");

        // Duration (Deezer preview is 30s)
        holder.tvDuration.setText("0:30");

        // Likes and plays (random for demo)
        holder.tvLikes.setText(generateRandomLikes());
        holder.tvPlays.setText(generateRandomPlays());

        // Load album cover with Glide
        if (song.cover != null && !song.cover.isEmpty()) {
            Glide.with(context)
                    .load(song.cover)
                    .apply(new RequestOptions()
                            .transform(new RoundedCorners(16))
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .error(android.R.drawable.ic_menu_report_image))
                    .into(holder.ivAlbumCover);
        } else {
            holder.ivAlbumCover.setImageResource(android.R.drawable.ic_menu_gallery);
            Log.w(TAG, "⚠ No cover for position " + position);
        }

        // Click event - Open PlayerActivity with FULL PLAYLIST
        holder.itemView.setOnClickListener(v -> {
            Log.d(TAG, "========== SONG CLICK ==========");
            Log.d(TAG, "Title: " + song.title);
            Log.d(TAG, "Position: " + position + "/" + songs.size());
            Log.d(TAG, "================================");

            if (song.audio == null || song.audio.isEmpty()) {
                Toast.makeText(context, "❌ Không có preview cho bài này!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Prepare playlist data
            ArrayList<String> playlistTitles = new ArrayList<>();
            ArrayList<String> playlistArtists = new ArrayList<>();
            ArrayList<String> playlistCovers = new ArrayList<>();
            ArrayList<String> playlistPreviews = new ArrayList<>();

            for (Song s : songs) {
                if (s.audio != null && !s.audio.isEmpty()) {
                    playlistTitles.add(s.title != null ? s.title : "Unknown");
                    playlistArtists.add(s.artist != null ? s.artist : "Unknown");
                    playlistCovers.add(s.cover != null ? s.cover : "");
                    playlistPreviews.add(s.audio);
                }
            }

            // Open PlayerActivity with full playlist
            Intent intent = new Intent(context, PlayerActivity.class);

            // Pass playlist data
            intent.putStringArrayListExtra("playlist_titles", playlistTitles);
            intent.putStringArrayListExtra("playlist_artists", playlistArtists);
            intent.putStringArrayListExtra("playlist_covers", playlistCovers);
            intent.putStringArrayListExtra("playlist_previews", playlistPreviews);
            intent.putExtra("current_index", findIndexInFilteredList(position));

            context.startActivity(intent);
            Log.d(TAG, "✅ PlayerActivity started with " + playlistTitles.size() + " songs");
        });

        // More button
        holder.btnMore.setOnClickListener(v -> {
            Toast.makeText(context, "⋮ " + song.title, Toast.LENGTH_SHORT).show();
        });

        // Add subtle animation on bind
        holder.itemView.setAlpha(0f);
        holder.itemView.setTranslationY(50f);
        holder.itemView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .setStartDelay(position * 30L)
                .start();
    }

    private int findIndexInFilteredList(int originalPosition) {
        int filteredIndex = 0;
        for (int i = 0; i <= originalPosition && i < songs.size(); i++) {
            if (songs.get(i).audio != null && !songs.get(i).audio.isEmpty()) {
                if (i == originalPosition) {
                    return filteredIndex;
                }
                filteredIndex++;
            }
        }
        return 0;
    }

    @Override
    public int getItemCount() {
        return songs != null ? songs.size() : 0;
    }

    private String generateRandomLikes() {
        double likes = 1.0 + (Math.random() * 4.0); // 1.0M - 5.0M
        return String.format("%.1fM", likes);
    }

    private String generateRandomPlays() {
        double plays = 10.0 + (Math.random() * 40.0); // 10M - 50M
        return String.format("%.1fM", plays);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank;
        ImageView ivAlbumCover;
        TextView tvSongTitle;
        TextView tvArtist;
        TextView tvDuration;
        TextView tvLikes;
        TextView tvPlays;
        TextView btnMore;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvRank = itemView.findViewById(R.id.tvRank);
            ivAlbumCover = itemView.findViewById(R.id.ivAlbumCover);
            tvSongTitle = itemView.findViewById(R.id.tvSongTitle);
            tvArtist = itemView.findViewById(R.id.tvArtist);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvLikes = itemView.findViewById(R.id.tvLikes);
            tvPlays = itemView.findViewById(R.id.tvPlays);
            btnMore = itemView.findViewById(R.id.btnMore);

            // Check for missing views
            if (tvRank == null) Log.e(TAG, "❌ tvRank NOT FOUND!");
            if (ivAlbumCover == null) Log.e(TAG, "❌ ivAlbumCover NOT FOUND!");
            if (tvSongTitle == null) Log.e(TAG, "❌ tvSongTitle NOT FOUND!");
            if (tvArtist == null) Log.e(TAG, "❌ tvArtist NOT FOUND!");
        }
    }
}