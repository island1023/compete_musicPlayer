package com.example.musicplayer;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ProfileActivity extends AppCompatActivity {

    private ImageView ivAvatar;
    private TextView tvNickname, tvUid, tvGender, tvAge, tvBirthday, tvSignature;

    private UserApiService apiService;
    private String currentUid;
    private String currentAvatarUrl = "";

    // 用于保存当前的全部真实数据，防止被重置为 0
    private int currentGender = 0;
    private long currentBirthday = 0;
    private String currentNickname = "";
    private String currentSignature = "";
    private int currentProvince = 0; // 极度重要
    private int currentCity = 0;     // 极度重要

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        currentUid = SpUtils.getUserId(this);
        initViews();
        initRetrofit();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!currentUid.isEmpty()) fetchUserProfile();
    }

    private void initViews() {
        ivAvatar = findViewById(R.id.iv_profile_avatar);
        tvNickname = findViewById(R.id.tv_profile_nickname);
        tvUid = findViewById(R.id.tv_profile_uid);
        tvGender = findViewById(R.id.tv_profile_gender);
        tvAge = findViewById(R.id.tv_profile_age);
        tvBirthday = findViewById(R.id.tv_profile_birthday);
        tvSignature = findViewById(R.id.tv_profile_signature);

        tvUid.setText(currentUid);
    }

    private void initRetrofit() {
        apiService = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:3000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(UserApiService.class);
    }

    private void setupListeners() {
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        findViewById(R.id.rl_avatar_container).setOnClickListener(v -> {
            if (currentAvatarUrl.isEmpty()) return;
            Intent intent = new Intent(this, AvatarViewActivity.class);
            intent.putExtra("avatarUrl", currentAvatarUrl);
            startActivity(intent);
        });

        findViewById(R.id.ll_nickname).setOnClickListener(v -> showEditDialog("修改昵称", currentNickname, 1));
        findViewById(R.id.ll_signature).setOnClickListener(v -> showEditDialog("修改简介", currentSignature, 2));

        findViewById(R.id.ll_gender).setOnClickListener(v -> {
            String[] genders = {"保密", "男", "女"};
            new AlertDialog.Builder(this)
                    .setTitle("选择性别")
                    .setSingleChoiceItems(genders, currentGender, (dialog, which) -> {
                        currentGender = which;
                        updateProfileOnServer();
                        dialog.dismiss();
                    }).show();
        });

        findViewById(R.id.ll_birthday).setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            if (currentBirthday > 0) calendar.setTimeInMillis(currentBirthday);
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                currentBirthday = calendar.getTimeInMillis();
                updateProfileOnServer();
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void showEditDialog(String title, String defaultText, int type) {
        EditText editText = new EditText(this);
        editText.setText(defaultText);
        new AlertDialog.Builder(this).setTitle(title).setView(editText)
                .setPositiveButton("保存", (dialog, which) -> {
                    String text = editText.getText().toString().trim();
                    if (type == 1) currentNickname = text;
                    else if (type == 2) currentSignature = text;
                    updateProfileOnServer();
                }).setNegativeButton("取消", null).show();
    }

    private void fetchUserProfile() {
        apiService.getUserDetail(currentUid).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        long createDays = jsonObject.optLong("createDays", 0);
                        long createTime = jsonObject.optLong("createTime", 0);

                        JSONObject profile = jsonObject.getJSONObject("profile");
                        currentNickname = profile.optString("nickname");
                        currentAvatarUrl = profile.optString("avatarUrl");
                        currentGender = profile.optInt("gender", 0);
                        currentBirthday = profile.optLong("birthday", 0);
                        currentSignature = profile.optString("signature", "");

                        // 🌟 保存原本的省份和城市ID，防止更新时被重置
                        currentProvince = profile.optInt("province", 0);
                        currentCity = profile.optInt("city", 0);

                        // 【新增本地覆盖逻辑】
                        MusicDbHelper dbHelper = new MusicDbHelper(ProfileActivity.this);
                        Cursor cursor = dbHelper.getUserProfile(currentUid);
                        if (cursor.moveToFirst()) {
                            String localNick = cursor.getString(cursor.getColumnIndexOrThrow("nickname"));
                            if (localNick != null) currentNickname = localNick;

                            int localGender = cursor.getInt(cursor.getColumnIndexOrThrow("gender"));
                            if (localGender != -1) currentGender = localGender;

                            long localBirth = cursor.getLong(cursor.getColumnIndexOrThrow("birthday"));
                            if (localBirth != -1) currentBirthday = localBirth;

                            String localSig = cursor.getString(cursor.getColumnIndexOrThrow("signature"));
                            if (localSig != null) currentSignature = localSig;

                            String localAvatarPath = cursor.getString(cursor.getColumnIndexOrThrow("avatarPath"));
                            if (localAvatarPath != null) currentAvatarUrl = localAvatarPath; // 使用本地路径
                        }
                        cursor.close();
                        dbHelper.close();

                        runOnUiThread(() -> {
                            Glide.with(ProfileActivity.this).load(currentAvatarUrl).circleCrop().error(R.drawable.ic_logo).into(ivAvatar);
                            tvNickname.setText(currentNickname);
                            tvGender.setText(currentGender == 1 ? "男" : (currentGender == 2 ? "女" : "保密"));
                            if (currentBirthday > 0) tvBirthday.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(currentBirthday)));
                            if (createTime > 0) tvAge.setText(createDays + "天(" + new SimpleDateFormat("yyyy年MM月", Locale.getDefault()).format(new Date(createTime)) + "创建)");
                            tvSignature.setText(currentSignature.isEmpty() ? "介绍一下自己吧" : currentSignature);
                        });
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }

    private void updateProfileOnServer() {
        // 【新增保存到本地】
        // 注：由于使用的是数据库replace，未修改的信息为防止设为空，保留current参数
        // 传递当前头像路径，以免执行修改其他资料导致头像记录丢失
        MusicDbHelper dbHelper = new MusicDbHelper(this);
        dbHelper.saveUserProfile(currentUid, currentNickname, currentGender, currentBirthday, currentSignature, currentAvatarUrl);
        dbHelper.close();

        Toast.makeText(this, "资料已保存", Toast.LENGTH_SHORT).show();
        fetchUserProfile(); // 本地刷新UI

    }
}