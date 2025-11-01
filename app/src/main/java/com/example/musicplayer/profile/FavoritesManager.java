package com.example.musicplayer.profile;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class FavoritesManager {
    private static final String PREFS_NAME = "MusicPlayerPrefs";
    private static final String KEY_FAVORITES = "favorite_songs";
    private static FavoritesManager instance;
    private SharedPreferences prefs;
    private Gson gson;
    private Set<FavoritesChangeListener> listeners;

    public interface FavoritesChangeListener {
        void onFavoritesChanged();
    }

    private FavoritesManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        listeners = new HashSet<>();
    }

    public static synchronized FavoritesManager getInstance(Context context) {
        if (instance == null) {
            instance = new FavoritesManager(context);
        }
        return instance;
    }

    public void addListener(FavoritesChangeListener listener) {
        listeners.add(listener);
    }

    public void removeListener(FavoritesChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (FavoritesChangeListener listener : listeners) {
            listener.onFavoritesChanged();
        }
    }

    public static class FavoriteSong {
        public String title;
        public String artist;
        public String cover;
        public String preview;
        public long timestamp;

        public FavoriteSong(String title, String artist, String cover, String preview) {
            this.title = title;
            this.artist = artist;
            this.cover = cover;
            this.preview = preview;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public boolean addFavorite(String title, String artist, String cover, String preview) {
        ArrayList<FavoriteSong> favorites = getFavorites();

        // Kiểm tra đã tồn tại chưa
        for (FavoriteSong song : favorites) {
            if (song.title.equals(title) && song.artist.equals(artist)) {
                return false; // Đã tồn tại
            }
        }

        favorites.add(new FavoriteSong(title, artist, cover, preview));
        saveFavorites(favorites);
        notifyListeners();
        return true;
    }

    public boolean removeFavorite(String title, String artist) {
        ArrayList<FavoriteSong> favorites = getFavorites();
        boolean removed = false;

        for (int i = favorites.size() - 1; i >= 0; i--) {
            FavoriteSong song = favorites.get(i);
            if (song.title.equals(title) && song.artist.equals(artist)) {
                favorites.remove(i);
                removed = true;
                break;
            }
        }

        if (removed) {
            saveFavorites(favorites);
            notifyListeners();
        }
        return removed;
    }

    public boolean isFavorite(String title, String artist) {
        ArrayList<FavoriteSong> favorites = getFavorites();
        for (FavoriteSong song : favorites) {
            if (song.title.equals(title) && song.artist.equals(artist)) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<FavoriteSong> getFavorites() {
        String json = prefs.getString(KEY_FAVORITES, null);
        if (json == null) {
            return new ArrayList<>();
        }

        Type type = new TypeToken<ArrayList<FavoriteSong>>(){}.getType();
        ArrayList<FavoriteSong> favorites = gson.fromJson(json, type);
        return favorites != null ? favorites : new ArrayList<>();
    }

    private void saveFavorites(ArrayList<FavoriteSong> favorites) {
        String json = gson.toJson(favorites);
        prefs.edit().putString(KEY_FAVORITES, json).apply();
    }

    public int getFavoritesCount() {
        return getFavorites().size();
    }

    public void clearAllFavorites() {
        prefs.edit().remove(KEY_FAVORITES).apply();
        notifyListeners();
    }
}