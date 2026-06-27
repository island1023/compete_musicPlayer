package com.example.musicplayer;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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

public class PasswordInputActivity extends AppCompatActivity {

    private TextView tvBack, tvPhoneDisplay;
    private EditText etPasswordInput;
    private Button btnPasswordLogin;

    private String phoneNumber;
    private AuthApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_input);

        phoneNumber = getIntent().getStringExtra("phone");
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(this, "手机号获取失败", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        initRetrofit();
        setupListeners();
    }

    private void initViews() {
        tvBack = findViewById(R.id.tv_back);
        tvPhoneDisplay = findViewById(R.id.tv_phone_display);
        etPasswordInput = findViewById(R.id.et_password_input);
        btnPasswordLogin = findViewById(R.id.btn_password_login);
        tvPhoneDisplay.setText("手机号：+86 " + phoneNumber);
    }

    private void initRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:3000/") // 请替换为你的实际后端地址
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(AuthApiService.class);
    }

    private void setupListeners() {
        tvBack.setOnClickListener(v -> finish());
        etPasswordInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    btnPasswordLogin.setEnabled(true);
                    btnPasswordLogin.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.RED));
                } else {
                    btnPasswordLogin.setEnabled(false);
                    btnPasswordLogin.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#CCCCCC")));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnPasswordLogin.setOnClickListener(v -> {
            String password = etPasswordInput.getText().toString();
            doLogin(password);
        });
    }

    private void doLogin(String password) {
        apiService.loginCellphone(phoneNumber, password, "").enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonStr = response.body().string();
                        JSONObject jsonObject = new JSONObject(jsonStr);

                        // 1. 提取 UID
                        String userId = jsonObject.getJSONObject("profile").getString("userId");

                        // 2. 保存 UID 到本地
                        SpUtils.saveUserId(PasswordInputActivity.this, userId);

                        Toast.makeText(PasswordInputActivity.this, "登录成功！", Toast.LENGTH_SHORT).show();

                        // 3. 跳转主页
                        Intent intent = new Intent(PasswordInputActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(PasswordInputActivity.this, "登录解析异常", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(PasswordInputActivity.this, "登录失败：密码错误或账号不存在", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(PasswordInputActivity.this, "网络请求失败", Toast.LENGTH_SHORT).show();
            }
        });
    }
}