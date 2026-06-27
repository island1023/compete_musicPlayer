package com.example.musicplayer;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CaptchaVerifyActivity extends AppCompatActivity {

    private TextView tvBack, tvPhoneHint, tvResendTimer;
    private EditText etCaptchaInput;
    private Button btnVerifyLogin;

    private String phoneNumber;
    private AuthApiService apiService;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_captcha_verify);

        phoneNumber = getIntent().getStringExtra("phone");
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(this, "手机号数据丢失", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        initRetrofit();
        setupListeners();
        sendCaptcha();
    }

    private void initViews() {
        tvBack = findViewById(R.id.tv_back);
        tvPhoneHint = findViewById(R.id.tv_phone_hint);
        tvResendTimer = findViewById(R.id.tv_resend_timer);
        etCaptchaInput = findViewById(R.id.et_captcha_input);
        btnVerifyLogin = findViewById(R.id.btn_verify_login);
        tvPhoneHint.setText("验证码已发送至 +86 " + phoneNumber);
    }

    private void initRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:3000/") // TODO: 替换为实际地址
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(AuthApiService.class);
    }

    private void setupListeners() {
        tvBack.setOnClickListener(v -> finish());
        etCaptchaInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= 4) {
                    btnVerifyLogin.setEnabled(true);
                    btnVerifyLogin.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.RED));
                } else {
                    btnVerifyLogin.setEnabled(false);
                    btnVerifyLogin.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#CCCCCC")));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        tvResendTimer.setOnClickListener(v -> sendCaptcha());
        btnVerifyLogin.setOnClickListener(v -> {
            String captcha = etCaptchaInput.getText().toString().trim();
            doLogin(captcha);
        });
    }

    private void sendCaptcha() {
        startCountdownTimer();
        apiService.sendCaptcha(phoneNumber).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(CaptchaVerifyActivity.this, "验证码已发送", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(CaptchaVerifyActivity.this, "发送失败，请稍后重试", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(CaptchaVerifyActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void doLogin(String captcha) {
        apiService.loginCellphone(phoneNumber, "", captcha).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonStr = response.body().string();
                        JSONObject jsonObject = new JSONObject(jsonStr);

                        // 1. 提取 UID
                        String userId = jsonObject.getJSONObject("profile").getString("userId");

                        // 2. 保存 UID 到本地
                        SpUtils.saveUserId(CaptchaVerifyActivity.this, userId);

                        Toast.makeText(CaptchaVerifyActivity.this, "登录成功！", Toast.LENGTH_SHORT).show();

                        // 3. 跳转主页 (注意这里的上下文是 CaptchaVerifyActivity.this)
                        Intent intent = new Intent(CaptchaVerifyActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(CaptchaVerifyActivity.this, "登录解析异常", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(CaptchaVerifyActivity.this, "验证码错误或过期", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(CaptchaVerifyActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startCountdownTimer() {
        tvResendTimer.setEnabled(false);
        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvResendTimer.setText((millisUntilFinished / 1000) + "s 后重发");
                tvResendTimer.setTextColor(Color.parseColor("#888888"));
            }
            @Override
            public void onFinish() {
                tvResendTimer.setEnabled(true);
                tvResendTimer.setText("重新发送");
                tvResendTimer.setTextColor(Color.parseColor("#000000"));
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}