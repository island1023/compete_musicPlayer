package com.example.musicplayer;

import android.content.Context;
import android.content.SharedPreferences;

public class SpUtils {
    private static final String SP_NAME = "music_config";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_COOKIE = "user_cookie"; // 新增 Cookie 键名

    // 保存 UID
    public static void saveUserId(Context context, String userId) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_USER_ID, userId).apply();
    }

    // 获取 UID
    public static String getUserId(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_USER_ID, "");
    }

    // 🌟 核心：保存 Cookie 字符串
    public static void saveCookie(Context context, String cookie) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_COOKIE, cookie).apply();
    }

    // 🌟 核心：获取保存的 Cookie
    public static String getCookie(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_COOKIE, "");
    }

    // 退出登录时清除所有数据
    public static void clearAll(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit().clear().apply();
    }
}