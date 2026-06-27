package com.example.musicplayer;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import okhttp3.Dispatcher;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String BASE_URL = "http://10.0.2.2:3000/"; // 你的 Node.js 服务地址
    private static Retrofit retrofit = null;

    // 1. 兼容你原来的登录和扫码页面
    public static AuthApiService getApi(Context context) {
        initRetrofit(context);
        return retrofit.create(AuthApiService.class);
    }

    // 2. 新增泛型方法，专门用于请求不同的接口文件 (如 HomeApiService)
    public static <T> T getService(Context context, Class<T> serviceClass) {
        initRetrofit(context);
        return retrofit.create(serviceClass);
    }

    // 3. 初始化并绑定 Cookie 拦截器和并发控制器
    private static void initRetrofit(Context context) {
        if (retrofit == null) {

            // 提升 OkHttp 的并发请求上限
            Dispatcher dispatcher = new Dispatcher();
            dispatcher.setMaxRequests(30);
            dispatcher.setMaxRequestsPerHost(15);

            OkHttpClient client = new OkHttpClient.Builder()
                    .dispatcher(dispatcher)
                    .addInterceptor(new ReadCookiesInterceptor(context)) // 注入 Cookie
                    .addInterceptor(new SaveCookiesInterceptor(context)) // 智能保存 Cookie
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
    }

    // ==================== 拦截器：智能读取并拼接 Cookie ====================
    private static class ReadCookiesInterceptor implements Interceptor {
        private Context context;
        public ReadCookiesInterceptor(Context context) { this.context = context; }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request.Builder builder = chain.request().newBuilder();
            SharedPreferences prefs = context.getSharedPreferences("CookiePrefs", Context.MODE_PRIVATE);
            Set<String> cookies = prefs.getStringSet("cookies", new HashSet<>());

            // 🌟 优化：将所有 Cookie 拼接成一个标准的字符串，格式为 "a=1; b=2;"
            if (!cookies.isEmpty()) {
                StringBuilder cookieHeader = new StringBuilder();
                for (String cookie : cookies) {
                    cookieHeader.append(cookie).append("; ");
                }
                // 去除末尾多余的 "; "
                if (cookieHeader.length() > 0) {
                    cookieHeader.setLength(cookieHeader.length() - 2);
                }
                builder.addHeader("Cookie", cookieHeader.toString());
            }
            return chain.proceed(builder.build());
        }
    }

    // ==================== 拦截器：智能合并保存 Cookie ====================
    private static class SaveCookiesInterceptor implements Interceptor {
        private Context context;
        public SaveCookiesInterceptor(Context context) { this.context = context; }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Response originalResponse = chain.proceed(chain.request());

            if (!originalResponse.headers("Set-Cookie").isEmpty()) {
                SharedPreferences prefs = context.getSharedPreferences("CookiePrefs", Context.MODE_PRIVATE);

                // 1. 获取本地已有的 Cookie 集合，并创建一个新的集合用于修改 (不能直接修改 SharedPreferences 返回的集合)
                Set<String> existingCookies = prefs.getStringSet("cookies", new HashSet<>());
                Set<String> mergedCookies = new HashSet<>(existingCookies);

                for (String header : originalResponse.headers("Set-Cookie")) {
                    // 2. 剥离掉无关属性（比如 Path=/, Expires=...），只保留核心的 "名=值" (如 MUSIC_U=xxxx)
                    String cookieKeyValue = header.split(";")[0];
                    String cookieName = cookieKeyValue.split("=")[0];

                    // 3. 智能合并：遍历旧集合，如果有同名的旧 Cookie，就把它删掉
                    String toRemove = null;
                    for (String existing : mergedCookies) {
                        if (existing.startsWith(cookieName + "=")) {
                            toRemove = existing;
                            break;
                        }
                    }
                    if (toRemove != null) {
                        mergedCookies.remove(toRemove);
                    }

                    // 4. 把最新的 "名=值" 加进去
                    mergedCookies.add(cookieKeyValue);
                }

                // 5. 将合并后的完整 Cookie 存回本地硬盘
                prefs.edit().putStringSet("cookies", mergedCookies).apply();
            }
            return originalResponse;
        }
    }
}