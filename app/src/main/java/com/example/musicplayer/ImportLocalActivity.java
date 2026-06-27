package com.example.musicplayer;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImportLocalActivity extends AppCompatActivity {

    private ListView lvLocalSongs;
    private List<Map<String, String>> localMusicList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_local);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        lvLocalSongs = findViewById(R.id.lv_local_songs);

        checkAllFilesPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 从设置页面授权回来后，重新检查并加载
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            if (localMusicList.isEmpty()) {
                loadLocalMusicDirectly();
            }
        }
    }

    // 🌟 核心：申请所有文件访问权限
    private void checkAllFilesPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 及以上
            if (!Environment.isExternalStorageManager()) {
                Toast.makeText(this, "请授予所有文件管理权限，以便扫描本地音乐", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            } else {
                loadLocalMusicDirectly();
            }
        } else {
            // Android 10 及以下，直接扫描
            loadLocalMusicDirectly();
        }
    }

    // 🌟 核心：直接扫描文件夹，不等待系统媒体库刷新
    private void loadLocalMusicDirectly() {
        localMusicList.clear();

        // 指定扫描的目录
        File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        scanDirectoryForMp3(musicDir);
        scanDirectoryForMp3(downloadDir);

        if (!localMusicList.isEmpty()) {
            SimpleAdapter adapter = new SimpleAdapter(
                    this, localMusicList,
                    android.R.layout.simple_list_item_2,
                    new String[]{"title", "path"},
                    new int[]{android.R.id.text1, android.R.id.text2}
            );
            lvLocalSongs.setAdapter(adapter);

            // 🌟 纯本地播放：直接携带本地绝对路径跳转播放页
            lvLocalSongs.setOnItemClickListener((parent, view, position, id) -> {
                Map<String, String> clickedSong = localMusicList.get(position);
                String title = clickedSong.get("title");
                String absolutePath = clickedSong.get("path"); // 获取真实的本地文件绝对路径

                String cleanTitle = title.replace(".mp3", ""); // 去掉后缀作为歌名显示

                // 直接跳转到播放页，不再请求云端
                Intent intent = new Intent(ImportLocalActivity.this, PlayerActivity.class);

                // 巧妙借用：把本地绝对路径塞进 songId 字段传过去
                intent.putExtra("songId", absolutePath);
                intent.putExtra("songName", cleanTitle);
                intent.putExtra("artistName", "本地音乐");
                intent.putExtra("coverUrl", ""); // 本地音乐暂不显示网络封面

                // 新增一个极其重要的标记，告诉播放页这是本地音乐
                intent.putExtra("isLocal", true);

                // 同样支持上一首/下一首，把当前文件夹下所有 mp3 的路径打包传过去
                ArrayList<String> playlistPaths = new ArrayList<>();
                for (Map<String, String> item : localMusicList) {
                    playlistPaths.add(item.get("path"));
                }
                intent.putStringArrayListExtra("playlistIds", playlistPaths);
                intent.putExtra("currentIndex", position);

                startActivity(intent);
            });
        } else {
            Toast.makeText(this, "指定的文件夹中暂无 .mp3 文件", Toast.LENGTH_SHORT).show();
        }
    }

    // 递归查找 MP3 文件
    private void scanDirectoryForMp3(File dir) {
        if (dir != null && dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        // 暂不进行深度递归，防止卡顿
                        // scanDirectoryForMp3(file);
                    } else {
                        if (file.getName().toLowerCase().endsWith(".mp3")) {
                            Map<String, String> map = new HashMap<>();
                            map.put("title", file.getName()); // 文件名
                            map.put("path", file.getAbsolutePath()); // 绝对路径
                            localMusicList.add(map);
                        }
                    }
                }
            }
        }
    }
}