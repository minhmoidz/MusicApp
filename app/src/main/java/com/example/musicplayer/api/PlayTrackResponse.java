package com.example.musicplayer.api;

import com.google.gson.annotations.SerializedName;

/**
 * Response for POST /api/spotify/tracks/{track_id}/play
 * Tự động lưu lịch sử và trả về thông tin track
 */
public class PlayTrackResponse {
    @SerializedName("code")
    public int code;

    @SerializedName("message")
    public String message;

    @SerializedName("data")
    public TrackData data;

    public static class TrackData {
        @SerializedName("id")
        public String id;

        @SerializedName("name")
        public String name;

        @SerializedName("artists")
        public String[] artists;

        @SerializedName("album")
        public String album;

        @SerializedName("duration_ms")
        public int durationMs;

        @SerializedName("popularity")
        public int popularity;

        @SerializedName("preview_url")
        public String previewUrl;

        @SerializedName("spotify_url")
        public String spotifyUrl;

        @SerializedName("image_url")
        public String imageUrl;

        @SerializedName("release_date")
        public String releaseDate;
    }
}
