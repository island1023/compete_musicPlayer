package com.example.musicplayer;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface PlayerApiService {

    // 获取歌曲详情 (包含海报 al.picUrl)

    // 获取歌曲详情 (包含VIP鉴权和真实下载权限 privileges)
    @GET("/song/detail")
    Call<ResponseBody> getSongDetail(@Query("ids") String ids);

    // 获取音乐真实播放链接
    @GET("/song/url/v1")
    Call<ResponseBody> getSongUrl(@Query("id") String id, @Query("level") String level);

    // 获取逐字歌词
    @GET("/lyric/new")
    Call<ResponseBody> getLyricNew(@Query("id") String id);

    // 🌟 喜欢/取消喜欢歌曲
    @GET("/song/like")
    Call<ResponseBody> toggleLike(
            @Query("id") String id,
            @Query("uid") String uid,
            @Query("like") boolean like
    );

    // 获取歌曲红心用户数量
    @GET("/song/red/count")
    Call<ResponseBody> getRedCount(@Query("id") String id);

    // 检查音乐是否可用
    @GET("/check/music")
    Call<ResponseBody> checkMusic(@Query("id") String id);

    // 检查歌曲是否已喜欢 (传入 ids 数组字符串，如 "[12345]")
    @GET("/song/like/check")
    Call<ResponseBody> checkSongLike(@Query("ids") String ids);

    // 获取音乐基础播放链接 (保底降级使用)
    @GET("/song/url")
    Call<ResponseBody> getSongUrlFallback(@Query("id") String id);
    // 🌟 补充：获取歌手单曲（网易云接口：/artists?id=xxx）
    @GET("/artists")
    Call<ResponseBody> getArtistDetail(@Query("id") String id);

    // 获取客户端歌曲下载链接 - 新版
    @GET("/song/download/url/v1")
    Call<okhttp3.ResponseBody> getSongDownloadUrl(@retrofit2.http.Query("id") String id, @retrofit2.http.Query("level") String level);

}