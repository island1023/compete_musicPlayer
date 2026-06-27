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

public class LoginActivity extends AppCompatActivity {

    private TextView tvCountrySelector;
    private EditText etPhoneNumber;
    private Button btnCaptchaLogin, btnPasswordLogin, btnQrLogin;
    private AuthApiService authApiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🌟 核心：在加载布局前先执行登录检查 (静默登录)
        checkLoginAndNavigate();
    }

    private void checkLoginAndNavigate() {
        String savedUid = SpUtils.getUserId(this);
        String savedCookie = SpUtils.getCookie(this);

        android.util.Log.e("LoginCheck", "=========== 开始登录检查 ===========");
        android.util.Log.e("LoginCheck", "1. 当前本地保存的 UID: " + savedUid);
        android.util.Log.e("LoginCheck", "2. 当前本地保存的 Cookie 是否为空: " + savedCookie.isEmpty());

        if (!savedUid.isEmpty() && !savedCookie.isEmpty()) {
            // 本地有持久化凭证，向服务器验证 Cookie 是否过期
            verifyServerStatus();
        } else {
            // 本地无记录，加载登录页面
            initLoginView();
        }
    }

    private void verifyServerStatus() {
        authApiService = RetrofitClient.getApi(this);

        authApiService.getLoginStatus().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String bodyStr = response.body().string();
                        android.util.Log.e("LoginCheck", "5. /login/status 请求成功，服务器返回体: " + bodyStr); // 🌟 新增打印

                        JSONObject json = new JSONObject(bodyStr);
                        JSONObject data = json.optJSONObject("data");

                        if (data != null && data.has("profile") && !data.isNull("profile")) {
                            android.util.Log.e("LoginCheck", "6. Profile 存在！验证通过，即将跳转主页！"); // 🌟 新增打印
                            navigateToMain();
                        } else {
                            android.util.Log.e("LoginCheck", "6. Profile 为空或不存在，认为 Cookie 已失效，重置回登录页。"); // 🌟 新增打印
                            initLoginView();
                        }
                    } else {
                        android.util.Log.e("LoginCheck", "5. /login/status 接口报错，状态码: " + response.code()); // 🌟 新增打印
                        initLoginView();
                    }
                } catch (Exception e) {
                    android.util.Log.e("LoginCheck", "5. 解析 /login/status 发生异常: " + e.getMessage()); // 🌟 新增打印
                    e.printStackTrace();
                    initLoginView();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                android.util.Log.e("LoginCheck", "5. /login/status 网络请求失败，降级跳转主页。"); // 🌟 新增打印
                navigateToMain();
            }
        });
    }

    private void initLoginView() {
        setContentView(R.layout.activity_login);
        initViews();
        setupListeners();
    }

    private void initViews() {
        tvCountrySelector = findViewById(R.id.tv_country_selector);
        etPhoneNumber = findViewById(R.id.et_phone_number);
        btnCaptchaLogin = findViewById(R.id.btn_login_captcha);
        btnPasswordLogin = findViewById(R.id.btn_login_password);
        btnQrLogin = findViewById(R.id.btn_login_qr); // 恢复二维码登录按钮

        checkInputStatus(""); // 初始化按钮状态
    }

    private void setupListeners() {
        // 1. 国家区号选择
        tvCountrySelector.setOnClickListener(v -> {
            CountrySelectFragment bottomSheet = new CountrySelectFragment();
            bottomSheet.setOnCountrySelectListener(countryCode -> {
                tvCountrySelector.setText(countryCode);
                checkInputStatus(etPhoneNumber.getText().toString());
            });
            bottomSheet.show(getSupportFragmentManager(), "CountrySelect");
        });

        // 2. 监听手机号输入
        etPhoneNumber.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkInputStatus(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // 3. 验证码按钮点击
        btnCaptchaLogin.setOnClickListener(v -> {
            Intent intent = new Intent(this, CaptchaVerifyActivity.class);
            intent.putExtra("phone", etPhoneNumber.getText().toString());
            startActivity(intent);
        });

        // 4. 密码按钮点击
        btnPasswordLogin.setOnClickListener(v -> {
            Intent intent = new Intent(this, PasswordInputActivity.class);
            intent.putExtra("phone", etPhoneNumber.getText().toString());
            startActivity(intent);
        });

        // 5. 二维码登录按钮点击
        if (btnQrLogin != null) {
            btnQrLogin.setOnClickListener(v -> {
                startActivity(new Intent(this, QRLoginActivity.class));
            });
        }
    }

    private void checkInputStatus(String phone) {
        String currentCode = tvCountrySelector.getText().toString();
        boolean isValid = currentCode.equals("+86") ? phone.length() == 11 : phone.length() > 5;
        int color = isValid ? Color.RED : Color.parseColor("#CCCCCC");

        btnCaptchaLogin.setEnabled(isValid);
        btnCaptchaLogin.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        btnPasswordLogin.setEnabled(isValid);
        btnPasswordLogin.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
























/*package com.example.musicplayer;

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

public class LoginActivity extends AppCompatActivity {

    private TextView tvCountrySelector;
    private EditText etPhoneNumber;
    private Button btnCaptchaLogin, btnPasswordLogin, btnQrLogin;
    private AuthApiService authApiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🌟 核心：在加载布局前先执行登录检查 (静默登录)
        checkLoginAndNavigate();
    }

    private void checkLoginAndNavigate() {
        // 1. 先查本地是否有 UID
        String savedUid = SpUtils.getUserId(this);
        if (savedUid != null && !savedUid.isEmpty()) {
            // 2. 本地有记录，尝试去服务器验证状态
            verifyServerStatus();
        } else {
            // 本地无记录，加载登录页面
            initLoginView();
        }
    }

    private void verifyServerStatus() {
        initRetrofit();
        authApiService.getLoginStatus().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject json = new JSONObject(response.body().string());
                        JSONObject data = json.optJSONObject("data");
                        if (data != null && data.has("profile") && !data.isNull("profile")) {
                            // 登录有效，直接进入
                            navigateToMain();
                        } else {
                            initLoginView();
                        }
                    } else {
                        initLoginView();
                    }
                } catch (Exception e) {
                    initLoginView();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // 网络异常但本地有UID时，允许进入主页
                navigateToMain();
            }
        });
    }

    private void initLoginView() {
        setContentView(R.layout.activity_login);
        initRetrofit();
        initViews();
        setupListeners();
    }

    private void initViews() {
        tvCountrySelector = findViewById(R.id.tv_country_selector);
        etPhoneNumber = findViewById(R.id.et_phone_number);
        btnCaptchaLogin = findViewById(R.id.btn_login_captcha);
        btnPasswordLogin = findViewById(R.id.btn_login_password);
        btnQrLogin = findViewById(R.id.btn_login_qr); // 🌟 二维码按钮

        // 初始化按钮状态
        checkInputStatus("");
    }

    private void initRetrofit() {
        if (authApiService != null) return;
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:3000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        authApiService = retrofit.create(AuthApiService.class);
    }

    private void setupListeners() {
        // 1. 国家区号选择
        tvCountrySelector.setOnClickListener(v -> {
            CountrySelectFragment bottomSheet = new CountrySelectFragment();
            bottomSheet.setOnCountrySelectListener(countryCode -> {
                tvCountrySelector.setText(countryCode);
                checkInputStatus(etPhoneNumber.getText().toString());
            });
            bottomSheet.show(getSupportFragmentManager(), "CountrySelect");
        });

        // 2. 监听手机号输入
        etPhoneNumber.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkInputStatus(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // 3. 验证码按钮点击
        btnCaptchaLogin.setOnClickListener(v -> {
            Intent intent = new Intent(this, CaptchaVerifyActivity.class);
            intent.putExtra("phone", etPhoneNumber.getText().toString());
            startActivity(intent);
        });

        // 4. 密码按钮点击
        btnPasswordLogin.setOnClickListener(v -> {
            Intent intent = new Intent(this, PasswordInputActivity.class);
            intent.putExtra("phone", etPhoneNumber.getText().toString());
            startActivity(intent);
        });

        // 🌟 5. 二维码登录按钮点击
        if (btnQrLogin != null) {
            btnQrLogin.setOnClickListener(v -> {
                // 跳转到专门处理扫码登录的 Activity
                startActivity(new Intent(this, QRLoginActivity.class));
            });
        }
    }

    private void checkInputStatus(String phone) {
        String currentCode = tvCountrySelector.getText().toString();
        boolean isValid = currentCode.equals("+86") ? phone.length() == 11 : phone.length() > 5;
        int color = isValid ? Color.RED : Color.parseColor("#CCCCCC");

        btnCaptchaLogin.setEnabled(isValid);
        btnCaptchaLogin.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        btnPasswordLogin.setEnabled(isValid);
        btnPasswordLogin.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}

 */