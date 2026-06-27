package com.example.musicplayer;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface HomeApiService {

    // ==================== 1. 搜索模块 ====================
    @GET("/cloudsearch") // 综合搜索 (默认推荐用 /cloudsearch，比 /search 全)
    Call<ResponseBody> search(@Query("keywords") String keywords, @Query("limit") int limit, @Query("offset") int offset, @Query("type") int type);

    @GET("/search/default") // 默认搜索关键词
    Call<ResponseBody> getSearchDefault();

    @GET("/search/hot") // 热搜列表(简略)
    Call<ResponseBody> getSearchHot();

    @GET("/search/hot/detail") // 热搜列表(详细)
    Call<ResponseBody> getSearchHotDetail();

    @GET("/search/suggest") // 搜索建议
    Call<ResponseBody> getSearchSuggest(@Query("keywords") String keywords, @Query("type") String type);

    @GET("/search/multimatch") // 搜索多重匹配
    Call<ResponseBody> getSearchMultimatch(@Query("keywords") String keywords);

    // ==================== 2. 基础推荐与轮播 ====================
    @GET("/banner") // 轮播图
    Call<ResponseBody> getBanner(@Query("type") int type);

    @GET("/recommend/resource") // 每日推荐歌单 (需登录)
    Call<ResponseBody> getRecommendResource();

    @GET("/recommend/songs") // 每日推荐歌曲 (需登录)
    Call<ResponseBody> getRecommendSongs(@Query("afresh") boolean afresh);

    @GET("/recommend/songs/dislike") // 每日推荐歌曲-不感兴趣
    Call<ResponseBody> dislikeRecommendSong(@Query("id") String id);

    @GET("/personalized") // 推荐歌单 (甄选歌单)
    Call<ResponseBody> getPersonalized(@Query("limit") int limit);

    @GET("/personalized/newsong") // 推荐新音乐
    Call<ResponseBody> getPersonalizedNewsong(@Query("limit") int limit);

    @GET("/personalized/djprogram") // 推荐电台
    Call<ResponseBody> getPersonalizedDj();

    @GET("/program/recommend") // 推荐节目
    Call<ResponseBody> getProgramRecommend(@Query("limit") int limit, @Query("offset") int offset);

    // ==================== 3. 新碟与歌手 ====================
    @GET("/top/album") // 新碟上架
    Call<ResponseBody> getTopAlbum(@Query("area") String area, @Query("type") String type, @Query("year") int year, @Query("month") int month, @Query("limit") int limit, @Query("offset") int offset);

    //精品歌单
    @GET("/top/playlist/highquality")
    Call<ResponseBody> getHighqualityPlaylists(@Query("limit") int limit);

    @GET("/album/new") // 全部新碟
    Call<ResponseBody> getAlbumNew(@Query("area") String area, @Query("limit") int limit, @Query("offset") int offset);

    @GET("/album/newest") // 最新专辑
    Call<ResponseBody> getAlbumNewest();

    @GET("/top/artists") // 热门歌手
    Call<ResponseBody> getTopArtists(@Query("limit") int limit, @Query("offset") int offset);

    // ==================== 4. 排行榜 ====================
    @GET("/toplist") // 所有榜单摘要
    Call<ResponseBody> getToplist();

    @GET("/top/list") // 排行榜详情 (传入榜单id)
    Call<ResponseBody> getTopListDetail(@Query("id") String id);

    @GET("/toplist/detail") // 所有榜单内容摘要
    Call<ResponseBody> getToplistContentDetail();

    @GET("/toplist/artist") // 歌手榜
    Call<ResponseBody> getToplistArtist(@Query("type") int type);

    // ==================== 5. 数字专辑 ====================
    @GET("/album/list") // 数字专辑-新碟上架
    Call<ResponseBody> getDigitalAlbumList(@Query("limit") int limit, @Query("offset") int offset);

    @GET("/album/songsaleboard") // 数字专辑&单曲-榜单
    Call<ResponseBody> getAlbumSongSaleBoard(@Query("limit") int limit, @Query("offset") int offset, @Query("albumType") int albumType, @Query("type") String type);

    @GET("/album/list/style") // 数字专辑-语种风格馆
    Call<ResponseBody> getAlbumStyleList(@Query("area") String area, @Query("limit") int limit, @Query("offset") int offset);

    @GET("/album/detail") // 数字专辑详情
    Call<ResponseBody> getDigitalAlbumDetail(@Query("id") String id);

    @GET("/digitalAlbum/purchased") // 我的数字专辑
    Call<ResponseBody> getPurchasedDigitalAlbum(@Query("limit") int limit);

    @GET("/digitalAlbum/ordering") // 购买数字专辑
    Call<ResponseBody> orderDigitalAlbum(@Query("id") String id, @Query("payment") int payment, @Query("quantity") int quantity);

    // ==================== 6. 最近播放 ====================
    @GET("/record/recent/song") // 最近播放-歌曲
    Call<ResponseBody> getRecentSong(@Query("limit") int limit);

    @GET("/record/recent/video") // 最近播放-视频
    Call<ResponseBody> getRecentVideo(@Query("limit") int limit);

    @GET("/record/recent/voice") // 最近播放-声音
    Call<ResponseBody> getRecentVoice(@Query("limit") int limit);

    @GET("/record/recent/playlist") // 最近播放-歌单
    Call<ResponseBody> getRecentPlaylist(@Query("limit") int limit);

    @GET("/record/recent/album") // 最近播放-专辑
    Call<ResponseBody> getRecentAlbum(@Query("limit") int limit);

    @GET("/record/recent/dj") // 最近播放-播客
    Call<ResponseBody> getRecentDj(@Query("limit") int limit);

    // ==================== 7. 庞大的电台(播客)模块 ====================
    @GET("/dj/banner") // 电台轮播图
    Call<ResponseBody> getDjBanner();

    @GET("/dj/personalize/recommend") // 电台个性推荐
    Call<ResponseBody> getDjPersonalizeRecommend(@Query("limit") int limit);

    @GET("/dj/subscriber") // 电台订阅者列表
    Call<ResponseBody> getDjSubscriber(@Query("id") String id, @Query("limit") int limit, @Query("time") long time);

    @GET("/user/audio") // 用户电台
    Call<ResponseBody> getUserAudio(@Query("uid") String uid);

    @GET("/dj/hot") // 热门电台
    Call<ResponseBody> getDjHot(@Query("limit") int limit, @Query("offset") int offset);

    @GET("/dj/program/toplist") // 电台-节目榜
    Call<ResponseBody> getDjProgramToplist(@Query("limit") int limit, @Query("offset") int offset);

    @GET("/dj/toplist/pay") // 电台-付费精品
    Call<ResponseBody> getDjPayToplist(@Query("limit") int limit);

    @GET("/dj/program/toplist/hours") // 电台-24小时节目榜
    Call<ResponseBody> getDjProgramToplistHours(@Query("limit") int limit);

    @GET("/dj/toplist/hours") // 电台-24小时主播榜
    Call<ResponseBody> getDjToplistHours(@Query("limit") int limit);

    @GET("/dj/toplist/newcomer") // 电台-主播新人榜
    Call<ResponseBody> getDjToplistNewcomer(@Query("limit") int limit);

    @GET("/dj/toplist/popular") // 电台-最热主播榜
    Call<ResponseBody> getDjToplistPopular(@Query("limit") int limit);

    @GET("/dj/toplist") // 电台-新晋/热门榜
    Call<ResponseBody> getDjToplist(@Query("type") String type, @Query("limit") int limit, @Query("offset") int offset);

    @GET("/dj/radio/hot") // 类别热门电台
    Call<ResponseBody> getDjRadioHot(@Query("cateId") String cateId, @Query("limit") int limit, @Query("offset") int offset);

    @GET("/dj/recommend") // 电台-推荐
    Call<ResponseBody> getDjRecommend();

    @GET("/dj/catelist") // 电台-分类
    Call<ResponseBody> getDjCatelist();

    @GET("/dj/recommend/type") // 电台-分类推荐
    Call<ResponseBody> getDjRecommendByType(@Query("type") int type);

    @GET("/dj/sub") // 电台-订阅与取消
    Call<ResponseBody> subDj(@Query("rid") String rid, @Query("t") int t);

    @GET("/dj/sublist") // 电台订阅列表
    Call<ResponseBody> getDjSublist();

    @GET("/dj/paygift") // 电台-付费精选
    Call<ResponseBody> getDjPaygift(@Query("limit") int limit, @Query("offset") int offset);

    @GET("/dj/category/excludehot") // 电台-非热门类型
    Call<ResponseBody> getDjCategoryExcludeHot();

    @GET("/dj/category/recommend") // 电台-推荐类型
    Call<ResponseBody> getDjCategoryRecommend();

    @GET("/dj/today/perfered") // 电台-今日优选
    Call<ResponseBody> getDjTodayPerfered();

    @GET("/dj/detail") // 电台-详情
    Call<ResponseBody> getDjDetail(@Query("rid") String rid);

    @GET("/dj/program") // 电台-节目列表
    Call<ResponseBody> getDjProgram(@Query("rid") String rid, @Query("limit") int limit, @Query("offset") int offset, @Query("asc") boolean asc);

    @GET("/dj/program/detail") // 电台-节目详情
    Call<ResponseBody> getDjProgramDetail(@Query("id") String id);

    // ==================== 1. 补充：获取专辑内容 ====================
    // 说明：传入专辑 id，获取专辑信息和包含的歌曲列表
    @GET("/album")
    Call<ResponseBody> getAlbumDetail(@Query("id") String id);

    // ==================== 2. 补充：获取歌曲播放链接 (新版) ====================
    // 说明：传入单曲 id（电台节目则是 mainTrackId），获取真实 mp3 url
    @GET("/song/url/v1")
    Call<ResponseBody> getSongUrlV1(@Query("id") String id, @Query("level") String level);

    // ==================== 3. 补充：获取歌曲是否可用 (版权/VIP校验) ====================
    @GET("/check/music")
    Call<ResponseBody> checkMusic(@Query("id") String id);

    @GET("/playlist/detail") // 歌单详情
    Call<ResponseBody> getPlaylistDetail(@Query("id") String id);

    // 🌟 补充：获取歌手单曲（网易云接口：/artists?id=xxx）
    @GET("/artists")
    Call<ResponseBody> getArtistDetail(@Query("id") String id);

}