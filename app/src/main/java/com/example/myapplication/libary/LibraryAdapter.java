package com.example.myapplication.libary;

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
import com.example.myapplication.R;

import java.util.List;

public class LibraryAdapter extends RecyclerView.Adapter<LibraryAdapter.ViewHolder> {

    private Context context;
    private List<LibraryItem> items;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(LibraryItem item);

        void onFavoriteClick(LibraryItem item);
    }

    public LibraryAdapter(Context context, List<LibraryItem> items, OnItemClickListener listener, FavoritesManager favoritesManager) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_library, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LibraryItem item = items.get(position);

        holder.tvTitle.setText(item.getTitle());
        holder.tvSubtitle.setText(item.getSubtitle());

        // Load ảnh với Glide và bo tròn góc
        RequestOptions options = new RequestOptions()
                .transform(new RoundedCorners(item.getType() == LibraryItem.Type.ARTIST ? 200 : 16));

        Glide.with(context)
                .load(item.getImageUrl())
                .apply(options)
                .placeholder(R.drawable.ic_music_placeholder)
                .into(holder.ivCover);

        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvTitle, tvSubtitle;
        ImageView ivMore;

        ViewHolder(View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.ivCover);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
            ivMore = itemView.findViewById(R.id.ivMore);
        }
    }
}