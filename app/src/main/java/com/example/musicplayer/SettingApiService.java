package com.example.musicplayer;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;

public interface SettingApiService {
    // 获取用户设置
    @GET("/setting")
    Call<ResponseBody> getUserSettings();

    // 退出登录
    @GET("/logout")
    Call<ResponseBody> logout();
}