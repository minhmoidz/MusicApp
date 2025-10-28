package com.example.myapplication.libary;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Quản lý danh sách yêu thích (bài hát, album, nghệ sĩ).
 * Lưu trữ dữ liệu bằng SharedPreferences ở dạng JSON.
 */
public class FavoritesManager {
    private static final String PREFS_NAME = "FavoritesPrefs";
    private static final String KEY_FAVORITES = "favorites";
    private static final String KEY_FAVORITE_IDS = "favorite_ids";

    private SharedPreferences prefs;
    private Set<String> favoriteIds;

    public FavoritesManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadFavoriteIds();
    }

    /**
     * Đọc danh sách ID yêu thích từ SharedPreferences.
     * Tạo bản sao để tránh lỗi ghi trực tiếp vào Set gốc.
     */
    private void loadFavoriteIds() {
        Set<String> savedSet = prefs.getStringSet(KEY_FAVORITE_IDS, new HashSet<>());
        favoriteIds = new HashSet<>(savedSet);
    }

    /**
     * Thêm 1 mục yêu thích mới.
     */
    public void addFavorite(LibraryItem item) {
        List<LibraryItem> favorites = getAllFavorites();

        // Kiểm tra trùng ID
        for (LibraryItem fav : favorites) {
            if (fav.getId().equals(item.getId())) {
                return; // Đã tồn tại, không thêm lại
            }
        }

        favorites.add(item);
        saveFavorites(favorites);

        favoriteIds.add(item.getId());
        saveFavoriteIds();

        Log.d("FavoritesManager", "Đã thêm vào yêu thích: " + item.getTitle());
    }

    /**
     * Xóa 1 mục yêu thích theo ID.
     */
    public void removeFavorite(String itemId) {
        List<LibraryItem> favorites = getAllFavorites();
        List<LibraryItem> newFavorites = new ArrayList<>();

        for (LibraryItem item : favorites) {
            if (!item.getId().equals(itemId)) {
                newFavorites.add(item);
            }
        }

        saveFavorites(newFavorites);
        favoriteIds.remove(itemId);
        saveFavoriteIds();

        Log.d("FavoritesManager", "Đã xóa khỏi yêu thích: " + itemId);
    }

    /**
     * Kiểm tra xem 1 mục có nằm trong danh sách yêu thích không.
     */
    public boolean isFavorite(String itemId) {
        if (favoriteIds == null) loadFavoriteIds();
        return favoriteIds.contains(itemId);
    }

    /**
     * Lấy toàn bộ danh sách yêu thích (ở dạng LibraryItem).
     */
    public List<LibraryItem> getAllFavorites() {
        String json = prefs.getString(KEY_FAVORITES, "[]");
        List<LibraryItem> favorites = new ArrayList<>();

        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);

                LibraryItem item = new LibraryItem(
                        obj.optString("id", ""),
                        obj.optString("title", "Không rõ tiêu đề"),
                        obj.optString("subtitle", ""),
                        obj.optString("imageUrl", ""),
                        obj.optString("previewUrl", ""),
                        LibraryItem.Type.valueOf(obj.optString("type", "SONG"))
                );
                favorites.add(item);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return favorites;
    }

    /**
     * Lưu danh sách yêu thích vào SharedPreferences.
     */
    private void saveFavorites(List<LibraryItem> favorites) {
        JSONArray array = new JSONArray();

        for (LibraryItem item : favorites) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("id", item.getId());
                obj.put("title", item.getTitle());
                obj.put("subtitle", item.getSubtitle());
                obj.put("imageUrl", item.getImageUrl());
                obj.put("previewUrl", item.getPreviewUrl());
                obj.put("type", item.getType().name());
                array.put(obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        prefs.edit().putString(KEY_FAVORITES, array.toString()).apply();
    }

    /**
     * Lưu danh sách ID yêu thích (để kiểm tra nhanh).
     */
    private void saveFavoriteIds() {
        prefs.edit().putStringSet(KEY_FAVORITE_IDS, new HashSet<>(favoriteIds)).apply();
    }

    /**
     * Xóa toàn bộ danh sách yêu thích (nếu cần reset).
     */
    public void clearAllFavorites() {
        prefs.edit()
                .remove(KEY_FAVORITES)
                .remove(KEY_FAVORITE_IDS)
                .apply();
        favoriteIds.clear();
        Log.d("FavoritesManager", "Đã xóa toàn bộ danh sách yêu thích.");
    }
}
