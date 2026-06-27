package com.example.musicplayer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SettingsActivity extends AppCompatActivity {

    private ImageView ivBack;
    private TextView tvLogout;
    private SettingApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initViews();
        initRetrofit();
        setupListeners();

        // 可选：加载用户设置状态，验证 Cookie 有效性
        loadUserSettings();
    }

    private void initViews() {
        ivBack = findViewById(R.id.iv_back);
        tvLogout = findViewById(R.id.tv_logout);
    }

    private void initRetrofit() {
        apiService = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:3000/") // 请替换为实际的 IP
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(SettingApiService.class);
    }

    private void setupListeners() {
        // 返回上一页
        ivBack.setOnClickListener(v -> finish());

        // 退出登录逻辑
        tvLogout.setOnClickListener(v -> performLogout());

        // 账号与安全点击提示 (示例)
        findViewById(R.id.tv_account_security).setOnClickListener(v ->
                Toast.makeText(SettingsActivity.this, "进入账号与安全设置", Toast.LENGTH_SHORT).show()
        );
    }

    private void loadUserSettings() {
        apiService.getUserSettings().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    // 数据加载成功，说明当前登录状态正常
                    // 可以解析 json 获取安全等级、绑定状态等并在 UI 上渲染
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // 网络错误
            }
        });
    }

    private void performLogout() {
        // 1. 调用网易云的登出接口，清除服务器侧的 session
        apiService.logout().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                // 无论接口是否成功，本地都强制退出
                clearLocalDataAndGoToLogin();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // 网络失败也强制退出
                clearLocalDataAndGoToLogin();
            }
        });
    }

    private void clearLocalDataAndGoToLogin() {
        // 2. 清除本地保存的 UID (依赖我们最早写的 SpUtils 工具类)
        SpUtils.saveUserId(this, "");

        Toast.makeText(this, "已退出登录", Toast.LENGTH_SHORT).show();

        // 3. 跳转回登录首页，并清空之前所有的 Activity 栈
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        // 4. 结束当前页面
        finish();
    }
}