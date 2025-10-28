package com.example.myapplication;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface DeezerApi {

    // Lấy danh sách bài hát hot (chart)
    @GET("chart/0/tracks")
    Call<TrackResponse> getChart();

    // Tìm kiếm bài hát
    @GET("search")
    Call<TrackResponse> searchTrack(@Query("q") String query);
}