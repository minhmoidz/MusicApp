package com.example.myapplication.playlist;

public class Playlist {
    public String id;
    public String name;
    public String cover;
    public int songCount;

    public Playlist(String id, String name, String cover, int songCount) {
        this.id = id;
        this.name = name;
        this.cover = cover;
        this.songCount = songCount;
    }
    }