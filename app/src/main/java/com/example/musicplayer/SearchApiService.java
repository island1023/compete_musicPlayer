package com.example.musicplayer;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface SearchApiService {
    // 获取默认搜索关键词 (Hint显示)
    @GET("/search/default")
    Call<ResponseBody> getDefaultSearchKeyword();

    // 综合搜索
    @GET("/cloudsearch")
    Call<ResponseBody> search(@Query("keywords") String keywords, @Query("type") int type);

    // 核心：获取所有榜单内容摘要 (用于渲染带歌曲列表的榜单卡片)
    @GET("/toplist/detail")
    Call<ResponseBody> getToplistDetail();

    @GET("/top/list")
    Call<ResponseBody> getTopList(@Query("id") String id);
    @GET("search")
    Call<ResponseBody> searchSongs(@Query("keywords") String keywords);
}