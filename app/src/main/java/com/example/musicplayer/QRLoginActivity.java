package com.example.musicplayer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class QRLoginActivity extends AppCompatActivity {

    private ImageView ivQrCode;
    private TextView tvQrStatus;
    private ProgressBar pbLoading;
    private Button btnRefresh;

    private String qrKey;
    private Handler handler = new Handler();
    private boolean isPolling = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_login);

        ivQrCode = findViewById(R.id.iv_qr_code);
        tvQrStatus = findViewById(R.id.tv_qr_status);
        pbLoading = findViewById(R.id.pb_loading);
        btnRefresh = findViewById(R.id.btn_refresh_qr);

        btnRefresh.setOnClickListener(v -> getQrKey());

        getQrKey();
    }

    private void getQrKey() {
        btnRefresh.setVisibility(View.GONE);
        pbLoading.setVisibility(View.VISIBLE);
        ivQrCode.setImageBitmap(null);
        tvQrStatus.setText("正在生成二维码...");

        // 🌟 修改：使用统一的单例获取接口，确保 Cookie 共享

        //RetrofitClient.getApi().getQrKey().enqueue(new Callback<ResponseBody>() {
        // 修改前：RetrofitClient.getApi().getQrKey().enqueue(new Callback<ResponseBody>() {
// 修改后 👇：
        RetrofitClient.getApi(QRLoginActivity.this).getQrKey().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    JSONObject json = new JSONObject(response.body().string());
                    qrKey = json.getJSONObject("data").getString("unikey");
                    createQrCode(qrKey);
                } catch (Exception e) {
                    tvQrStatus.setText("获取 Key 失败");
                    pbLoading.setVisibility(View.GONE);
                    btnRefresh.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {
                tvQrStatus.setText("网络异常");
                pbLoading.setVisibility(View.GONE);
                btnRefresh.setVisibility(View.VISIBLE);
            }
        });
    }

    private void createQrCode(String key) {
       // RetrofitClient.getApi().createQr(key, true).enqueue(new Callback<ResponseBody>() {
        // 修改前：RetrofitClient.getApi().createQr(key, true).enqueue(new Callback<ResponseBody>() {
// 修改后 👇：
        RetrofitClient.getApi(QRLoginActivity.this).createQr(key, true).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    JSONObject json = new JSONObject(response.body().string());
                    String qrimg = json.getJSONObject("data").getString("qrimg");
                    String base64Data = qrimg.split(",")[1];
                    byte[] decodedString = Base64.decode(base64Data, Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                    ivQrCode.setImageBitmap(decodedByte);
                    pbLoading.setVisibility(View.GONE);
                    tvQrStatus.setText("请使用网易云 App 扫码");
                    startPolling();
                } catch (Exception e) {
                    pbLoading.setVisibility(View.GONE);
                }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }

    private void startPolling() {
        isPolling = true;
        handler.post(pollingRunnable);
    }

    private void stopPolling() {
        isPolling = false;
        handler.removeCallbacks(pollingRunnable);
    }

    private Runnable pollingRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isPolling || isFinishing()) return;

            //RetrofitClient.getApi().checkQrStatus(qrKey).enqueue(new Callback<ResponseBody>() {
            // 修改前：RetrofitClient.getApi().checkQrStatus(qrKey).enqueue(new Callback<ResponseBody>() {
// 修改后 👇：
            RetrofitClient.getApi(QRLoginActivity.this).checkQrStatus(qrKey).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    try {
                        String body = response.body().string();
                        JSONObject json = new JSONObject(body);
                        int code = json.getInt("code");

                        if (code == 800) {
                            tvQrStatus.setText("二维码已过期");
                            stopPolling();
                            btnRefresh.setVisibility(View.VISIBLE);
                        } else if (code == 802) {
                            tvQrStatus.setText("扫码成功，请在手机确认");
                        } else if (code == 803) {
                            stopPolling();
                            tvQrStatus.setText("授权成功，正在进入...");

                            // 🌟 【核心修复】从 JSON 体中提取 Cookie 并保存
                            String cookieStr = json.optString("cookie");

                            // 兼容处理：尝试获取 Set-Cookie 头 (双重保险)
                            StringBuilder headerCookies = new StringBuilder();
                            for (String headerCookie : response.headers().values("Set-Cookie")) {
                                headerCookies.append(headerCookie).append(";");
                            }

                            // 哪边有数据就用哪边的
                            String finalCookie = !cookieStr.isEmpty() ? cookieStr : headerCookies.toString();

                            if (!finalCookie.isEmpty()) {
                                // 强制保存到本地 SpUtils
                                SpUtils.saveCookie(QRLoginActivity.this, finalCookie);
                                android.util.Log.e("LoginCheck", "扫码成功，成功拦截并保存 Cookie: " + finalCookie);
                            } else {
                                android.util.Log.e("LoginCheck", "警告：803返回了，但没有找到任何 Cookie!");
                            }

                            checkFinalStatus();
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }
                @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
            });

            if (isPolling) {
                handler.postDelayed(this, 3000);
            }
        }
    };

    private void checkFinalStatus() {
        //RetrofitClient.getApi().getLoginStatus().enqueue(new Callback<ResponseBody>() {
        // 修改前：RetrofitClient.getApi().getLoginStatus().enqueue(new Callback<ResponseBody>() {
// 修改后 👇：
        RetrofitClient.getApi(QRLoginActivity.this).getLoginStatus().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    String body = response.body().string();
                    JSONObject json = new JSONObject(body);
                    JSONObject data = json.optJSONObject("data");
                    JSONObject profile = (data != null) ? data.optJSONObject("profile") : json.optJSONObject("profile");

                    if (profile != null) {
                        String uid = profile.getString("userId");
                        SpUtils.saveUserId(QRLoginActivity.this, uid);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    // 🌟 无论服务器是否返回 profile，由于 Cookie 已经在单例 OkHttp 内存中，强制跳转
                    doJump();
                }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {
                doJump();
            }
        });
    }

    // 🌟 补全缺失的 doJump 方法
    private void doJump() {
        if (isFinishing()) return;
        Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(QRLoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public void onFinishClick(View view) {
        finish();
    }

    @Override
    protected void onDestroy() {
        stopPolling();
        super.onDestroy();
    }
}