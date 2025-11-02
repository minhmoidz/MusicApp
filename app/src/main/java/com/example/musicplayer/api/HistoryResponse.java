package com.example.musicplayer.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class HistoryResponse {
    @SerializedName("code")
    public int code;

    @SerializedName("message")
    public String message;

    @SerializedName("data")
    public HistoryData data;

    public static class HistoryData {
        @SerializedName("items")
        public List<HistoryItem> items;

        @SerializedName("total")
        public int total;

        @SerializedName("limit")
        public int limit;

        @SerializedName("offset")
        public int offset;
    }

    public static class HistoryItem {
        @SerializedName("id")
        public int id;

        @SerializedName("track_id")
        public String trackId;

        @SerializedName("track_name")
        public String trackName;

        @SerializedName("artist_name")
        public String artistName;

        @SerializedName("album")
        public String album;

        @SerializedName("duration_ms")
        public int durationMs;

        @SerializedName("listened_at")
        public String listenedAt;

        @SerializedName("play_duration_seconds")
        public int playDurationSeconds;
    }
}
