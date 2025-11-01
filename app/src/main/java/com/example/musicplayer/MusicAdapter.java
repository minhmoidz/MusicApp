package com.example.musicplayer; // (Giữ package của bạn)

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.ViewHolder> {

    private final Context context;
    private final List<Song> songs;

    // ----- THAY ĐỔI 1: Thêm 2 listener -----
    private OnItemClickListener itemClickListener;
    private OnOptionsClickListener optionsClickListener; // Listener mới cho nút 3 chấm

    // --- Interface cũ của bạn ---
    public interface OnItemClickListener {
        void onItemClick(String trackId);
    }

    // ----- THAY ĐỔI 2: Thêm interface mới -----
    public interface OnOptionsClickListener {
        // Gửi về cả bài hát VÀ view (nút 3 chấm) để PopupMenu biết neo vào đâu
        void onOptionsClick(Song song, View anchorView);
    }

    // --- Setter cũ của bạn ---
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    // ----- THAY ĐỔI 3: Thêm setter cho listener mới -----
    public void setOnOptionsClickListener(OnOptionsClickListener listener) {
        this.optionsClickListener = listener;
    }

    public MusicAdapter(Context context, List<Song> songs) {
        this.context = context;
        this.songs = songs;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_song, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Song song = songs.get(position);

        // ----- THAY ĐỔI 4: Tách logic bind và listener -----

        // 1. Chỉ gọi bind để gán dữ liệu
        holder.bind(song);

        // 2. Gán listener trực tiếp ở đây
        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                itemClickListener.onItemClick(song.id);
            }
        });

        // 3. Gán listener MỚI cho btnMore
        holder.btnMore.setOnClickListener(v -> {
            if (optionsClickListener != null) {
                optionsClickListener.onOptionsClick(song, holder.btnMore);
            }
        });
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank, tvSongTitle, tvArtist, tvDuration, tvLikes, tvPlays, btnMore;
        ImageView ivAlbumCover;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tvRank);
            tvSongTitle = itemView.findViewById(R.id.tvSongTitle);
            tvArtist = itemView.findViewById(R.id.tvArtist);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            ivAlbumCover = itemView.findViewById(R.id.ivAlbumCover);
            tvLikes = itemView.findViewById(R.id.tvLikes);
            tvPlays = itemView.findViewById(R.id.tvPlays);
            btnMore = itemView.findViewById(R.id.btnMore);
        }

        // ----- THAY ĐỔI 5: Đơn giản hóa hàm bind -----
        // (Bỏ tham số listener, vì đã xử lý ở onBindViewHolder)
        public void bind(final Song song) {
            tvRank.setText(String.valueOf(getAdapterPosition() + 1));
            tvSongTitle.setText(song.title);
            tvArtist.setText(song.artist);
            tvDuration.setText("0:30"); // Default preview duration

            if (getAdapterPosition() < 3) {
                tvRank.setTextColor(0xFFFFD700);
            } else {
                tvRank.setTextColor(0xFFB0B0B0);
            }

            Glide.with(itemView.getContext())
                    .load(song.cover)
                    .apply(new RequestOptions().transform(new RoundedCorners(16)))
                    .into(ivAlbumCover);

            // Đã xóa itemView.setOnClickListener khỏi đây
        }
    }
}