package com.example.musicplayer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AvatarViewActivity extends AppCompatActivity {

    private ImageView ivLargeAvatar;
    private UserApiService apiService;

    private final ActivityResultLauncher<Intent> photoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    ivLargeAvatar.setImageURI(imageUri);
                    uploadAvatarToServer(imageUri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_avatar_view);

        ivLargeAvatar = findViewById(R.id.iv_large_avatar);
        String avatarUrl = getIntent().getStringExtra("avatarUrl");
        Glide.with(this).load(avatarUrl).into(ivLargeAvatar);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        apiService = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:3000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(UserApiService.class);

        findViewById(R.id.btn_change_avatar).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            photoPickerLauncher.launch(intent);
        });
    }

    private void uploadAvatarToServer(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            // 加上时间戳，保证每次路径唯一，打破 Glide 缓存
            File localAvatarFile = new File(getFilesDir(), "local_avatar_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream localOut = new FileOutputStream(localAvatarFile);
            byte[] buf = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                localOut.write(buf, 0, len);
            }
            localOut.close();
            inputStream.close();

            MusicDbHelper db = new MusicDbHelper(this);
            db.saveUserProfile(SpUtils.getUserId(this), null, -1, -1, null, localAvatarFile.getAbsolutePath());
            db.close();

            Toast.makeText(AvatarViewActivity.this, "头像已更新！", Toast.LENGTH_SHORT).show();
            // 删除此处原有的 apiService.uploadAvatar(...).enqueue(...) 逻辑
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}