package com.example.musicplayer;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface UserApiService {

    /**
     * 1. 初始化昵称
     * 刚注册或首次登录时，用于设置用户的初始昵称。
     */
    @GET("/activate/init/profile")
    Call<ResponseBody> initProfile(@Query("nickname") String nickname);

    /**
     * 2. 获取用户详情 (极其重要，用于渲染页面头部)
     */
    @GET("/user/detail")
    Call<ResponseBody> getUserDetail(@Query("uid") String uid);

    /**
     * 3. 更新用户信息 (我的资料页修改)
     * 注意：province 和 city 如果不知怎么传，必须传用户原有的值，传0会导致保存失败。
     */
    @GET("/user/update")
    Call<ResponseBody> updateUser(
            @Query("gender") int gender,
            @Query("birthday") long birthday,
            @Query("nickname") String nickname,
            @Query("province") int province,
            @Query("city") int city,
            @Query("signature") String signature
    );

    /**
     * 4. 更新头像 (支持文件上传)
     */
    @Multipart
    @POST("/avatar/upload")
    Call<ResponseBody> uploadAvatar(
            @Query("imgSize") int imgSize,
            @Part MultipartBody.Part imgFile
    );

    /**
     * 5. 获取用户关注列表
     */
    @GET("/user/follows")
    Call<ResponseBody> getUserFollows(@Query("uid") String uid, @Query("limit") int limit, @Query("offset") int offset);

    /**
     * 6. 获取用户粉丝列表
     */
    @GET("/user/followeds")
    Call<ResponseBody> getUserFolloweds(@Query("uid") String uid, @Query("limit") int limit, @Query("offset") int offset);

    /**
     * 7. 关注 / 取消关注用户
     * @param id 目标用户的 ID
     * @param t 1 为关注，其他数字(如0)为取消关注
     */
    @GET("/follow")
    Call<ResponseBody> followUser(@Query("id") String id, @Query("t") int t);

    /**
     * 8. 获取用户播放记录 (修复最近播放)
     * @param uid 用户 ID
     * @param type 1: 返回 weekData (本周), 0: 返回 allData (所有时间)
     */
    @GET("/user/record")
    Call<ResponseBody> getUserRecord(@Query("uid") String uid, @Query("type") int type);

    /**
     * 9. 获取喜欢的音乐 (红心歌曲) ID 列表
     */
    @GET("/likelist")
    Call<ResponseBody> getLikeList(@Query("uid") String uid);

    @GET("/record/recent/song?limit=20")
    Call<ResponseBody> getRecentSongs();

    /**
     * 10. 批量获取歌曲详情 (用于喜欢的音乐列表展示)
     */
    @GET("/song/detail")
    Call<ResponseBody> getSongDetail(@Query("ids") String ids);

    @GET("/playlist/create")
    Call<ResponseBody> createPlaylist(@Query("name") String name);

    @GET("/search/match")
    Call<ResponseBody> matchLocalSong(@Query("title") String title, @Query("artist") String artist, @Query("duration") long duration);

    @GET("/user/dj")
    Call<ResponseBody> getUserDj(@Query("uid") String uid);

    @GET("/user/event")
    Call<ResponseBody> getUserEvent(@Query("uid") String uid, @Query("limit") int limit, @Query("lasttime") long lasttime);

    /**
     * 获取最近播放 - 歌曲
     * @param limit 返回数量 , 默认为 100
     */
    @GET("/record/recent/song")
    Call<ResponseBody> getRecentSongs(@Query("limit") int limit);


    /** * ==============================================
     * 以下为您本次需要新增的 API 接口
     * ==============================================
     */
    /**
     * 1. 新建歌单 (完善版)
     * @param name 歌单名
     * @param privacy 是否设置为隐私歌单，默认否，传 '10' 则设置成隐私歌单
     * @param type 歌单类型, 默认 'NORMAL', 传 'VIDEO' 则为视频歌单, 传 'SHARED' 则为共享歌单
     */
    @GET("/playlist/create")
    Call<ResponseBody> createPlaylist(
            @Query("name") String name,
            @Query("privacy") String privacy,
            @Query("type") String type
    );

    /**
     * 2. 删除歌单
     * @param id 歌单 id, 可传入多个用逗号隔开 (如: 2947311456,5013464397)
     */
    @GET("/playlist/delete")
    Call<ResponseBody> deletePlaylist(@Query("id") String id);

    /**
     * 3. 对歌单添加或删除歌曲
     * 注意：v4.29.7版本后，必须带上 timestamp 字段，否则可能报 502 不合法。
     * @param op 操作类型：增加单曲为 "add", 删除为 "del"
     * @param pid 目标歌单的 id
     * @param tracks 要操作的歌曲 id, 可传入多个用逗号隔开
     * @param timestamp 当前的时间戳 (System.currentTimeMillis())
     */
    @GET("/playlist/tracks")
    Call<ResponseBody> operatePlaylistTracks(
            @Query("op") String op,
            @Query("pid") String pid,
            @Query("tracks") String tracks,
            @Query("timestamp") long timestamp
    );

    /**
     * 4. 搜索音乐 (使用更全的 /cloudsearch)
     * @param keywords 搜索关键词 (如 "周杰伦 搁浅")
     * @param limit 返回数量 , 默认为 30
     * @param offset 偏移数量用于分页: (页数 - 1) * limit
     * @param type 搜索类型 (1:单曲, 10:专辑, 100:歌手, 1000:歌单, 1002:用户, 1004:MV, 1014:视频)
     */
    @GET("/cloudsearch")
    Call<ResponseBody> searchMusic(
            @Query("keywords") String keywords,
            @Query("limit") int limit,
            @Query("offset") int offset,
            @Query("type") int type
    );

    /**
     * 5. 获取歌单详情
     * 只能获取歌单的基础信息和最多10首歌，若要获取全部歌曲需配合 /playlist/track/all 使用。
     * @param id 歌单 id
     */
    @GET("/playlist/detail")
    Call<ResponseBody> getPlaylistDetail(@Query("id") String id);

    /**
     * 6. 获取歌单所有歌曲
     * 突破详情接口只给 10 首歌的限制，传入详情获取到的完整 trackIds 获取所有歌曲真实数据。
     * @param id 歌单 id
     * @param limit 限制获取歌曲的数量 (默认当前歌单全量)
     * @param offset 偏移量用于分页
     */
    @GET("/playlist/track/all")
    Call<ResponseBody> getPlaylistTracks(
            @Query("id") String id,
            @Query("limit") int limit,
            @Query("offset") int offset
    );

    /**
     * 7. 获取音乐真实播放链接 (新版 v1)
     * @param id 音乐 id (可传入多个用逗号隔开)
     * @param level 音质等级 (standard:标准, higher:较高, exhigh:极高, lossless:无损, hires:Hi-Res)
     */
    @GET("/song/url/v1")
    Call<ResponseBody> getMusicUrlV1(
            @Query("id") String id,
            @Query("level") String level
    );

    // 获取歌手热门单曲（网易云接口：/artists?id=xxx）
    @retrofit2.http.GET("/artists")
    retrofit2.Call<okhttp3.ResponseBody> getArtistDetail(@retrofit2.http.Query("id") String id);

}