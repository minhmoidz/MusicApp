package com.example.myapplication;

import java.util.List;

public class TrackResponse {
    public List<Track> data;

    public static class Track {
        public String title;
        public String preview; // link nhạc demo 30s
        public Artist artist;
        public Album album;
    }

    public static class Artist {
        public String name;
    }

    public static class Album {
        public String cover; // url cover
    }
}
