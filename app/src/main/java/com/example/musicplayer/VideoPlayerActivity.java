package com.example.musicplayer;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import org.json.JSONObject;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VideoPlayerActivity extends AppCompatActivity {
    private ExoPlayer player;
    private PlayerView playerView;
    private VideoApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        playerView = findViewById(R.id.video_player_view);
        ImageView ivBack = findViewById(R.id.iv_video_back);
        ivBack.setOnClickListener(v -> finish());

        // 初始化 API 服务
        apiService = RetrofitClient.getService(this, VideoApiService.class);

        // 获取上个页面传来的基础 ID 和 URL
        String videoId = getIntent().getStringExtra("videoId");
        String videoUrl = getIntent().getStringExtra("videoUrl");

        // 🌟 核心修复：从 Intent 中获取 MV 的标题、作者和封面URL
        String title = getIntent().getStringExtra("title");
        String creator = getIntent().getStringExtra("creator");
        String coverUrl = getIntent().getStringExtra("coverUrl");

        // 播放逻辑
        if (videoId != null && !videoId.isEmpty()) {
            // 优先调用 mv/url 接口获取真实地址，避开防盗链
            getRealVideoUrl(videoId);
        } else if (videoUrl != null && !videoUrl.isEmpty()) {
            // 备用方案：如果已有地址，直接尝试播放
            initPlayer(videoUrl);
        } else {
            Toast.makeText(this, "无法获取视频地址", Toast.LENGTH_SHORT).show();
            finish();
            return; // 地址都没有，就没必要存记录了
        }

        // 🌟 将播放记录存入本地数据库 (做了非空保护，防止上个页面没传对应参数导致崩溃)
        if (videoId != null && !videoId.isEmpty()) {
            MusicDbHelper dbHelper = new MusicDbHelper(this);
            String safeTitle = (title != null) ? title : "未知视频";
            String safeCreator = (creator != null) ? creator : "未知作者";
            String safeCover = (coverUrl != null) ? coverUrl : "";

            dbHelper.addMvHistory(videoId, safeTitle, safeCreator, safeCover);
            dbHelper.close();
        }
    }

    private void getRealVideoUrl(String mvId) {
        apiService.getMvUrl(mvId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        // 网易云 mv/url 接口返回结构通常是 data -> url
                        String realUrl = jsonObject.getJSONObject("data").optString("url");

                        if (realUrl != null && !realUrl.isEmpty()) {
                            runOnUiThread(() -> initPlayer(realUrl));
                        } else {
                            runOnUiThread(() -> Toast.makeText(VideoPlayerActivity.this, "解析地址失败", Toast.LENGTH_SHORT).show());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                runOnUiThread(() -> Toast.makeText(VideoPlayerActivity.this, "网络请求失败", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void initPlayer(String url) {
        if (url == null || url.isEmpty()) return;

        // 释放旧资源
        if (player != null) {
            player.release();
        }

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        // 🌟 优化：MediaItem 自动识别 URL 类型，不要手动写死 MimeType
        MediaItem mediaItem = MediaItem.fromUri(url);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}