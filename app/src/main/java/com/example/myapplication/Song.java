package com.example.myapplication;

public class Song {
    public String title;   // tên bài hát
    public String artist;  // tên ca sĩ
    public String cover;   // url cover ảnh
    public String audio;   // url nhạc (preview từ Deezer)
    public String lyrics;  // lời bài hát

    public Song(String title, String artist, String cover, String audio, String lyrics) {
        this.title = title;
        this.artist = artist;
        this.cover = cover;
        this.audio = audio;
        this.lyrics = lyrics;
    }

    // ===== THÊM GETTERS =====
    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getCover() {
        return cover;
    }

    public String getAudio() {
        return audio;
    }

    // Alias để dễ đọc
    public String getPreview() {
        return audio;
    }

    public String getLyrics() {
        return lyrics;
    }

    // ===== THÊM SETTERS =====
    public void setTitle(String title) {
        this.title = title;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public void setAudio(String audio) {
        this.audio = audio;
    }

    public void setLyrics(String lyrics) {
        this.lyrics = lyrics;
    }

    // Hàm tiện ích để map từ Deezer API object
    public static Song fromDeezerApi(SongApi apiSong) {
        return new Song(
                apiSong.title,
                apiSong.artist.name,
                apiSong.album.cover,
                apiSong.preview,
                "" // lyrics trống, cần API khác để lấy
        );
    }

    // Class phụ tương ứng dữ liệu API Deezer
    public static class SongApi {
        public String title;
        public Artist artist;
        public Album album;
        public String preview;

        public static class Artist {
            public String name;
        }

        public static class Album {
            public String cover;
        }
    }
}