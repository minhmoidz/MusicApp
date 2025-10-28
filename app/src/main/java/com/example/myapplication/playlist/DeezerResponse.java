package com.example.myapplication.playlist;

import java.util.List;

public class DeezerResponse {
    public TrackList tracks;

    public static class TrackList {
        public List<Track> data;
    }

    public static class Track {
        public String title;
        public String preview;
        public Artist artist;
        public Album album;
    }

    public static class Artist {
        public String name;
    }

    public static class Album {
        public String cover;
    }
}
