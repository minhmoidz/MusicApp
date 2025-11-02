package com.example.musicplayer.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Response for GET /api/history/stats
 */
public class HistoryStatsResponse {
    @SerializedName("code")
    public int code;

    @SerializedName("message")
    public String message;

    @SerializedName("data")
    public StatsData data;

    public static class StatsData {
        @SerializedName("total_plays")
        public int totalPlays;

        @SerializedName("unique_tracks")
        public int uniqueTracks;

        @SerializedName("top_tracks")
        public List<TopTrack> topTracks;

        @SerializedName("top_artists")
        public List<TopArtist> topArtists;
    }

    public static class TopTrack {
        @SerializedName("track_id")
        public String trackId;

        @SerializedName("track_name")
        public String trackName;

        @SerializedName("artist_name")
        public String artistName;

        @SerializedName("play_count")
        public int playCount;
    }

    public static class TopArtist {
        @SerializedName("artist_name")
        public String artistName;

        @SerializedName("play_count")
        public int playCount;
    }
}
