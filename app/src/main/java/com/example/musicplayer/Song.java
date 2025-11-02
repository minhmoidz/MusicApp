package com.example.musicplayer;

public class Song {
    public String id;      // Spotify Track ID
    public String title;   // Tên bài hát
    public String artist;  // Tên ca sĩ
    public String cover;   // URL ảnh bìa
    public String audio;   // URL nhạc preview
    public String lyrics;  // Lời bài hát
    public String albumName; // Tên album
    public int durationMs; // Độ dài bài hát (ms)
    public int popularity; // ADDED: Độ phổ biến
    public String spotifyUrl; // ADDED: Link Spotify

    public Song(String id, String title, String artist, String cover, String audio, String lyrics, String albumName, int durationMs, int popularity, String spotifyUrl) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.cover = cover;
        this.audio = audio;
        this.lyrics = lyrics;
        this.albumName = albumName;
        this.durationMs = durationMs;
        this.popularity = popularity;
        this.spotifyUrl = spotifyUrl;
    }
}
