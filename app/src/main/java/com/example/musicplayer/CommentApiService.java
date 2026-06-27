package com.example.musicplayer;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface CommentApiService {
    // 获取新版评论 (type: 0歌曲, 1MV, 5视频)
    @GET("/comment/new")
    Call<ResponseBody> getComments(
            @Query("type") int type,
            @Query("id") String id,
            @Query("sortType") int sortType,
            @Query("pageNo") int pageNo,
            @Query("pageSize") int pageSize,
            @Query("cursor") Long cursor
    );

    // 发送/回复/删除评论 (t: 1发送, 2回复, 0删除)
    @GET("/comment")
    Call<ResponseBody> handleComment(
            @Query("t") int t,
            @Query("type") int type,
            @Query("id") String id,
            @Query("content") String content,
            @Query("commentId") String commentId
    );

    // 评论点赞
    @GET("/comment/like")
    Call<ResponseBody> likeComment(
            @Query("type") int type,
            @Query("id") String id,
            @Query("cid") String cid,
            @Query("t") int t
    );
}