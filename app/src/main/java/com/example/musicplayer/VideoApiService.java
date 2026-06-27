package com.example.musicplayer;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface VideoApiService {
    // --- MV 相关 ---
    @GET("/top/mv")
    Call<ResponseBody> getMvRanking(@Query("area") String area, @Query("limit") int limit, @Query("offset") int offset);

    @GET("/mv/detail")
    Call<ResponseBody> getMvDetail(@Query("mvid") String mvid);

    @GET("/mv/url")
    Call<ResponseBody> getMvUrl(@Query("id") String id, @Query("r") int resolution);

    @GET("/mv/detail/info")
    Call<ResponseBody> getMvInfo(@Query("mvid") String mvid);

    // --- 视频相关 ---
    @GET("/video/timeline/recommend")
    Call<ResponseBody> getRecommendVideos(@Query("offset") int offset);

    @GET("/video/group/list")
    Call<ResponseBody> getVideoTags();

    @GET("/video/group")
    Call<ResponseBody> getVideosByTag(@Query("id") String id, @Query("offset") int offset);

    @GET("/video/detail")
    Call<ResponseBody> getVideoDetail(@Query("id") String id);

    @GET("/video/url")
    Call<ResponseBody> getVideoUrl(@Query("id") String id);

    @GET("/related/allvideo")
    Call<ResponseBody> getRelatedVideos(@Query("id") String id);

    // 在 VideoApiService.java 中添加
    @GET("/mv/url")
    Call<ResponseBody> getMvUrl(@Query("id") String id);
}