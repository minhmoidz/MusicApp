package com.example.musicplayer.api;

import com.example.musicplayer.login.LoginActivity;
import com.example.musicplayer.login.RegisterRequest;
import com.example.musicplayer.login.UpdateUserRequest;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface SpotifyApi {

    // --- Auth Endpoints ---
    @POST("/api/auth/login")
    Call<LoginActivity.AuthTokenResponse> login(@Body LoginActivity.LoginRequest loginRequest);

    @POST("/api/auth/register")
    Call<LoginActivity.AuthTokenResponse> register(@Body RegisterRequest registerRequest);

    @GET("/api/auth/me")
    Call<LoginActivity.AuthTokenResponse.User> getMe();

    @PUT("/api/auth/me")
    Call<Void> updateProfile(@Body UpdateUserRequest updateUserRequest);

    // --- History Endpoints ---

    // ⭐ RECOMMENDED: Play track & auto-save to history
    @POST("/api/spotify/tracks/{track_id}/play")
    Call<PlayTrackResponse> playTrack(
            @Path("track_id") String trackId,
            @Query("play_duration_seconds") int playDurationSeconds);

    // Manual: Add to history (fallback)
    @POST("/api/history")
    Call<Void> addHistoryRecord(@Body HistoryRecordRequest historyRecordRequest);

    @GET("/api/history")
    Call<HistoryResponse> getHistory(
            @Query("limit") int limit,
            @Query("offset") int offset,
            @Query("track_id") String trackId);

    @GET("/api/history/stats")
    Call<HistoryStatsResponse> getHistoryStats();

    // --- Music Endpoints ---
    @POST("/api/spotify/tracks/search")
    Call<SpotifySearchResponse> searchTracks(@Body JsonObject body);

    @POST("/api/spotify/recommendations")
    Call<List<SpotifyTrack>> getRecommendations(@Body JsonObject body);

    @GET("/api/spotify/tracks/{track_id}")
    Call<SpotifyTrack> getTrackDetails(@Path("track_id") String trackId);
}
