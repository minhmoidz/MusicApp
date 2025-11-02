package com.example.musicplayer.api;

import com.google.gson.annotations.SerializedName;

public class HistoryRecordRequest {
    @SerializedName("track_id")
    private final String trackId;

    @SerializedName("track_name")
    private final String trackName;

    @SerializedName("artist_name")
    private final String artistName;

    @SerializedName("album")
    private final String album;

    @SerializedName("duration_ms")
    private final int durationMs;

    @SerializedName("play_duration_seconds")
    private final int playDurationSeconds;

    public HistoryRecordRequest(String trackId, String trackName, String artistName, String album, int durationMs,
            int playDurationSeconds) {
        this.trackId = trackId;
        this.trackName = trackName;
        this.artistName = artistName;
        this.album = album;
        this.durationMs = durationMs;
        this.playDurationSeconds = playDurationSeconds;
    }

    // Getter methods to ensure proper serialization
    public String getTrackId() {
        return trackId;
    }

    public String getTrackName() {
        return trackName;
    }

    public String getArtistName() {
        return artistName;
    }

    public String getAlbum() {
        return album;
    }

    public int getDurationMs() {
        return durationMs;
    }

    public int getPlayDurationSeconds() {
        return playDurationSeconds;
    }
}
