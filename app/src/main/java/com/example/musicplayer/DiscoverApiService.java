package com.example.musicplayer;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface DiscoverApiService {
    // 获取曲风列表
    @GET("/style/list")
    Call<ResponseBody> getStyleList();

    // 获取热门歌单分类
    @GET("/playlist/hot")
    Call<ResponseBody> getHotPlaylistTags();

    // 根据曲风获取歌单
    @GET("/top/playlist")
    Call<ResponseBody> getPlaylistsByTag(@Query("cat") String cat, @Query("limit") int limit);

    // 🌟 新增：根据歌单 id 获取歌单详情及歌曲列表
    @GET("/playlist/detail")
    Call<ResponseBody> getPlaylistDetail(@Query("id") String id);
}