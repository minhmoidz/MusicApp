package com.example.myapplication.libary;

public class LibraryItem {
    public enum Type { SONG, ALBUM, ARTIST }

    private String id;
    private String title;
    private String subtitle;
    private String imageUrl;
    private String previewUrl;
    private Type type;

    public LibraryItem(String id, String title, String subtitle, String imageUrl, String previewUrl, Type type) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.imageUrl = imageUrl;
        this.previewUrl = previewUrl;
        this.type = type;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public String getImageUrl() { return imageUrl; }
    public String getPreviewUrl() { return previewUrl; }
    public Type getType() { return type; }
}