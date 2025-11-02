package com.example.musicplayer.chatbot;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.R;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private final List<ChatMessage> messages;

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        String role = msg.getRole().toLowerCase();

        // Ẩn tất cả
        holder.userCard.setVisibility(View.GONE);
        holder.botCard.setVisibility(View.GONE);
        holder.systemCard.setVisibility(View.GONE);

        // Hiển thị theo role
        switch (role) {
            case "user":
                holder.userCard.setVisibility(View.VISIBLE);
                holder.userContentView.setText(msg.getContent());
                break;

            case "assistant":
            case "bot":
                holder.botCard.setVisibility(View.VISIBLE);
                holder.botContentView.setText(msg.getContent());
                break;

            case "system":
                holder.systemCard.setVisibility(View.VISIBLE);
                holder.systemContentView.setText(msg.getContent());
                break;
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView userCard, botCard, systemCard;
        TextView userContentView, botContentView, systemContentView;

        ViewHolder(View itemView) {
            super(itemView);
            userCard = itemView.findViewById(R.id.userCard);
            userContentView = itemView.findViewById(R.id.userContentView);

            botCard = itemView.findViewById(R.id.botCard);
            botContentView = itemView.findViewById(R.id.botContentView);

            systemCard = itemView.findViewById(R.id.systemCard);
            systemContentView = itemView.findViewById(R.id.systemContentView);
        }
    }
}