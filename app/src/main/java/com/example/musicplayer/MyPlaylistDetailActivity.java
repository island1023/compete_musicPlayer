package com.example.musicplayer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class MyPlaylistDetailActivity extends AppCompatActivity {
    private String playlistId;
    private String playlistName;
    private ImageView ivCover;

    // 头像选择器
    private final ActivityResultLauncher<Intent> photoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    updateLocalCover(imageUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_playlist_detail);

        // 1. 获取传递过来的歌单基础数据
        playlistId = getIntent().getStringExtra("playlistId");
        playlistName = getIntent().getStringExtra("playlistName");
        String coverPath = getIntent().getStringExtra("playlistCover");

        // 2. 绑定返回按钮
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        // 3. 渲染头部封面和歌单名
        ivCover = findViewById(R.id.iv_cover);
        TextView tvTitle = findViewById(R.id.tv_title);
        tvTitle.setText(playlistName);

        if (coverPath != null && !coverPath.isEmpty()) {
            Glide.with(this).load(coverPath).into(ivCover);
        }

        // 4. 绑定点击更换封面功能
        findViewById(R.id.rl_cover_container).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            photoPickerLauncher.launch(intent);
        });

        // 5. 绑定删除本地歌单逻辑
        TextView tvDeletePlaylist = findViewById(R.id.tv_delete_playlist);
        if (tvDeletePlaylist != null) {
            tvDeletePlaylist.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("提示")
                        .setMessage("确定要删除本地歌单「" + playlistName + "」吗？")
                        .setPositiveButton("确定", (dialog, which) -> {
                            MusicDbHelper db = new MusicDbHelper(MyPlaylistDetailActivity.this);
                            // 执行数据库删除逻辑
                            db.deleteLocalPlaylist(playlistId);
                            db.close();

                            Toast.makeText(MyPlaylistDetailActivity.this, "歌单删除成功", Toast.LENGTH_SHORT).show();
                            finish(); // 删除后直接关闭并退出当前页面
                        })
                        .setNegativeButton("取消", null)
                        .show();
            });
        }

        // 6. 初始化下方歌曲列表并加载数据
        RecyclerView rvSongs = findViewById(R.id.rv_songs);
        rvSongs.setLayoutManager(new LinearLayoutManager(this));

        // 调用加载数据的方法（渲染刚才加入歌单的歌曲）
        loadLocalSongs(rvSongs);
    }

    // 每次回到页面时重新加载，确保新加的歌能立刻显示
    @Override
    protected void onResume() {
        super.onResume();
        RecyclerView rvSongs = findViewById(R.id.rv_songs);
        if (rvSongs != null && playlistId != null) {
            loadLocalSongs(rvSongs);
        }
    }

    // 更新本地封面
    private void updateLocalCover(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            // 本地保存封面
            File coverFile = new File(getFilesDir(), "cover_" + playlistId + ".jpg");
            FileOutputStream localOut = new FileOutputStream(coverFile);
            byte[] buf = new byte[1024]; int len;
            while ((len = inputStream.read(buf)) > 0) localOut.write(buf, 0, len);
            localOut.close(); inputStream.close();

            Glide.with(this).load(coverFile).into(ivCover);

            // 更新到数据库中的本地歌单表
            MusicDbHelper db = new MusicDbHelper(this);
            db.updateLocalPlaylistCover(playlistId, coverFile.getAbsolutePath());
            db.close();
            Toast.makeText(this, "封面更新成功", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 加载本地数据库中该歌单的歌曲
    private void loadLocalSongs(RecyclerView rvSongs) {
        MusicDbHelper db = new MusicDbHelper(this);
        List<Map<String, String>> songList = db.getPlaylistSongs(playlistId);
        db.close();

        // 绑定数据到列表
        rvSongs.setAdapter(new LocalSongAdapter(songList));
    }

    // 内部类：歌曲列表适配器
    private class LocalSongAdapter extends RecyclerView.Adapter<LocalSongAdapter.ViewHolder> {
        private List<Map<String, String>> data;

        public LocalSongAdapter(List<Map<String, String>> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // 复用单曲条目布局 item_song_result
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_song_result, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, String> song = data.get(position);
            holder.tvSongName.setText(song.get("name"));
            holder.tvArtistName.setText(song.get("artist"));

            // 点击该歌曲，跳转回 PlayerActivity 播放
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(MyPlaylistDetailActivity.this, PlayerActivity.class);
                intent.putExtra("songId", song.get("songId"));
                intent.putExtra("songName", song.get("name"));
                intent.putExtra("artistName", song.get("artist"));
                intent.putExtra("coverUrl", song.get("coverUrl"));
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return data == null ? 0 : data.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvSongName, tvArtistName;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvSongName = itemView.findViewById(R.id.tv_song_name);
                tvArtistName = itemView.findViewById(R.id.tv_artist_name);
            }
        }
    }
}