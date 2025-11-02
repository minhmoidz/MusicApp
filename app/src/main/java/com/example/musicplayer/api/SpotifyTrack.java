package com.example.musicplayer.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SpotifyTrack {
    @SerializedName("id")
    public String id;

    @SerializedName("name")
    public String name;

    @SerializedName("artists")
    public List<String> artists;

    @SerializedName("album")
    public String album;

    @SerializedName("duration_ms")
    public int durationMs;

    @SerializedName("popularity")
    public int popularity; // ADDED

    @SerializedName("preview_url")
    public String previewUrl;

    @SerializedName("spotify_url")
    public String spotifyUrl; // ADDED

    @SerializedName("image_url")
    public String imageUrl;

    public String getArtistsString() {
        if (artists == null || artists.isEmpty()) {
            return "Unknown Artist";
        }
        return String.join(", ", artists);
    }
}
