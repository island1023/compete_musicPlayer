package com.example.musicplayer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;
import java.util.Map;

public class PlaylistDetailActivity extends AppCompatActivity {

    private String playlistId;
    private String playlistName;
    private String playlistCover;
    private String type; // 🌟 1. 新增 type 变量接收上个页面的类型

    private ImageView ivBack;
    private ImageView ivCover;
    private TextView tvTitle;
    private RecyclerView rvSongs;

    private DiscoverApiService apiService;
    private TrackAdapter trackAdapter;
    private List<PlaylistTrackModel> trackList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 1. 绑定复用你的 activity_playlist_detail.xml 布局
        setContentView(R.layout.activity_playlist_detail);

        // 2. 接收上一个页面传过来的歌单基础数据
        playlistId = getIntent().getStringExtra("playlistId");
        playlistName = getIntent().getStringExtra("playlistName");
        playlistCover = getIntent().getStringExtra("playlistCover");
        type = getIntent().getStringExtra("type"); // 🌟 2. 接收 type 参

        // 3. 初始化视图控件
        ivBack = findViewById(R.id.iv_back);
        ivCover = findViewById(R.id.iv_cover);
        tvTitle = findViewById(R.id.tv_title);

        // 💡 提示：如果你的 XML 中 RecyclerView 的 ID 不是 rv_songs，请调整为与 XML 一致
        rvSongs = findViewById(R.id.rv_songs);
        if (rvSongs == null) {
            rvSongs = findViewById(getResources().getIdentifier("rv_playlist_songs", "id", getPackageName()));
        }

        // 4. 先用已有数据渲染头部（防止网络延迟导致空白）
        if (tvTitle != null && playlistName != null) tvTitle.setText(playlistName);
        if (ivCover != null && playlistCover != null) {
            Glide.with(this).load(playlistCover).into(ivCover);
        }

        // 5. 设置返回键
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> finish());
        }

        // 6. 配置歌曲列表RecyclerView
        if (rvSongs != null) {
            rvSongs.setLayoutManager(new LinearLayoutManager(this));
            trackAdapter = new TrackAdapter(trackList);
            rvSongs.setAdapter(trackAdapter);
        }

        // 7. 初始化网络请求并加载歌曲 (🌟 3. 修改这部分，进行条件分发)
        apiService = RetrofitClient.getService(this, DiscoverApiService.class);

        if ("radio".equals(type)) {
            loadRadioTracks(); // 如果是电台，调用电台接口
        } else if ("album".equals(type)) {
            loadAlbumTracks(); // 如果是专辑，调用专辑接口
        } else {
            loadPlaylistTracks(); // 默认当做歌单处理
        }

        TextView tvFavPlaylist = findViewById(R.id.tv_fav_playlist);
        if (tvFavPlaylist != null && playlistId != null) {

            // 🌟 1. 进入页面时，立刻查询本地数据库，获取真实的持久化收藏状态
            MusicDbHelper initialDb = new MusicDbHelper(this);
            // 使用数组是为了在底部的 lambda 表达式中可以修改这个变量
            final boolean[] isFavorited = {initialDb.isFavPlaylist(playlistId)};
            initialDb.close();

            // 🌟 2. 根据真实的数据库状态，初始化按钮样式
            if (isFavorited[0]) {
                tvFavPlaylist.setText("已收藏");
                tvFavPlaylist.setTextColor(android.graphics.Color.parseColor("#AAAAAA"));
            } else {
                tvFavPlaylist.setText("收 藏");
                tvFavPlaylist.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
            }

            // 🌟 3. 绑定点击事件，支持“收藏”和“取消收藏”的双向持久化切换
            tvFavPlaylist.setOnClickListener(v -> {
                // 状态反转：已收藏变未收藏，未收藏变已收藏
                isFavorited[0] = !isFavorited[0];

                // 执行数据库持久化（如果 isFavorited 为 true 就是插入，false 就是删除）
                MusicDbHelper dbHelper = new MusicDbHelper(PlaylistDetailActivity.this);
                dbHelper.toggleFavPlaylist(playlistId, playlistName, playlistCover, isFavorited[0]);
                dbHelper.close();

                // 更新界面与提示
                if (isFavorited[0]) {
                    Toast.makeText(PlaylistDetailActivity.this, "已收藏歌单", Toast.LENGTH_SHORT).show();
                    tvFavPlaylist.setText("已收藏");
                    tvFavPlaylist.setTextColor(android.graphics.Color.parseColor("#AAAAAA"));
                } else {
                    Toast.makeText(PlaylistDetailActivity.this, "已取消收藏", Toast.LENGTH_SHORT).show();
                    tvFavPlaylist.setText("收 藏");
                    tvFavPlaylist.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
                }
            });
        }
    }

    private void loadPlaylistTracks() {
        if (playlistId == null || playlistId.isEmpty()) return;

        // 🌟 核心修复：如果是本地创建的歌单，不请求云端服务器，直接本地渲染基础数据，防止解析崩溃
        if (playlistId.startsWith("LOCAL_")) {
            runOnUiThread(() -> {
                if (tvTitle != null && playlistName != null) tvTitle.setText(playlistName);
                if (ivCover != null) {
                    if (playlistCover != null && !playlistCover.isEmpty()) {
                        Glide.with(PlaylistDetailActivity.this).load(playlistCover).placeholder(R.drawable.ic_logo).error(R.drawable.ic_logo).into(ivCover);
                    } else {
                        ivCover.setImageResource(R.drawable.ic_logo); // 本地歌单默认图标兜底
                    }
                }
                // 我创建的歌单不需要显示“收藏”按钮，直接隐藏
                // 在 PlaylistDetailActivity.java 的 onCreate() 方法末尾进行替换和补充：
                TextView tvFavPlaylist = findViewById(R.id.tv_fav_playlist);
                if (tvFavPlaylist != null) {

                    // 🌟 核心状态持久化显现逻辑：进入页面时直接检索本地数据库
                    boolean hasFavorited = false;
                    if (playlistId != null) {
                        MusicDbHelper checkDb = new MusicDbHelper(this);
                        List<Map<String, String>> currentFavs = checkDb.getFavPlaylists();
                        checkDb.close();
                        if (currentFavs != null) {
                            for (Map<String, String> map : currentFavs) {
                                String savedPid = map.get("pid");
                                if (savedPid == null) savedPid = map.get("id");
                                if (playlistId.equals(savedPid)) {
                                    hasFavorited = true;
                                    break;
                                }
                            }
                        }
                    }

                    // 根据查验出来的数据库真实记录渲染收藏文本状态
                    if (hasFavorited) {
                        tvFavPlaylist.setText("已收藏");
                        tvFavPlaylist.setTextColor(android.graphics.Color.parseColor("#AAAAAA"));
                    } else {
                        tvFavPlaylist.setText("收 藏");
                        tvFavPlaylist.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
                    }

                    tvFavPlaylist.setOnClickListener(v -> {
                        if (playlistId == null) return;
                        MusicDbHelper dbHelper = new MusicDbHelper(PlaylistDetailActivity.this);
                        // 执行持久化收藏入库
                        dbHelper.toggleFavPlaylist(playlistId, playlistName, playlistCover, true);
                        dbHelper.close();

                        Toast.makeText(PlaylistDetailActivity.this, "已收藏歌单", Toast.LENGTH_SHORT).show();
                        tvFavPlaylist.setText("已收藏");
                        tvFavPlaylist.setTextColor(android.graphics.Color.parseColor("#AAAAAA"));
                    });
                }
            });

            trackList.clear();
            // 如果后续有本地歌曲关联表，可在此处读取本地数据库。目前先清空防止报错：
            runOnUiThread(() -> trackAdapter.notifyDataSetChanged());
            return;
        }

        // ⬇️ 以下为原有的云端网络歌单请求逻辑，保持不变
        apiService.getPlaylistDetail(playlistId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        JSONObject playlistObj = jsonObject.optJSONObject("playlist");

                        if (playlistObj != null) {
                            String realName = playlistObj.optString("name");
                            String realCover = playlistObj.optString("coverImgUrl");
                            playlistName = realName;
                            playlistCover = realCover;
                            runOnUiThread(() -> {
                                if (tvTitle != null) tvTitle.setText(realName);
                                if (ivCover != null) Glide.with(PlaylistDetailActivity.this).load(realCover).into(ivCover);
                            });

                            JSONArray tracksArray = playlistObj.optJSONArray("tracks");
                            if (tracksArray != null && tracksArray.length() > 0) {
                                trackList.clear();
                                for (int i = 0; i < tracksArray.length(); i++) {
                                    JSONObject trackObj = tracksArray.getJSONObject(i);
                                    PlaylistTrackModel model = new PlaylistTrackModel();
                                    model.id = trackObj.optString("id");
                                    model.name = trackObj.optString("name");

                                    JSONArray arArray = trackObj.optJSONArray("ar");
                                    if (arArray != null && arArray.length() > 0) {
                                        model.artist = arArray.getJSONObject(0).optString("name");
                                    } else {
                                        model.artist = "未知歌手";
                                    }

                                    JSONObject alObj = trackObj.optJSONObject("al");
                                    if (alObj != null) {
                                        model.coverUrl = alObj.optString("picUrl");
                                    } else {
                                        model.coverUrl = realCover;
                                    }
                                    trackList.add(model);
                                }
                                runOnUiThread(() -> trackAdapter.notifyDataSetChanged());
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        showToast("歌曲列表解析异常");
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                showToast("获取歌单详情失败");
            }
        });
    }

    // 🌟 新增：专门加载电台节目的方法
    private void loadRadioTracks() {
        if (playlistId == null || playlistId.isEmpty()) return;

        HomeApiService homeApiService = RetrofitClient.getService(this, HomeApiService.class);

        // 1. 获取电台详情 (更新头部UI)
        homeApiService.getDjDetail(playlistId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    JSONObject dataObj = jsonObject.optJSONObject("data");
                    if (dataObj != null) {
                        String realName = dataObj.optString("name");
                        String realCover = dataObj.optString("picUrl");
                        playlistName = realName;
                        playlistCover = realCover;
                        runOnUiThread(() -> {
                            if (tvTitle != null) tvTitle.setText(realName);
                            if (ivCover != null) Glide.with(PlaylistDetailActivity.this).load(realCover).into(ivCover);
                        });
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });

        // 2. 获取电台包含的节目列表 (歌曲列表)
        homeApiService.getDjProgram(playlistId, 50, 0, false).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    JSONArray programsArray = jsonObject.optJSONArray("programs");
                    if (programsArray != null && programsArray.length() > 0) {
                        trackList.clear();
                        for (int i = 0; i < programsArray.length(); i++) {
                            JSONObject programObj = programsArray.getJSONObject(i);
                            PlaylistTrackModel model = new PlaylistTrackModel();

                            // 电台节目的音频本体通常存放在 mainSong 里
                            JSONObject mainSong = programObj.optJSONObject("mainSong");
                            if (mainSong != null) {
                                model.id = mainSong.optString("id");
                                model.name = mainSong.optString("name");

                                JSONArray arArray = mainSong.optJSONArray("artists"); // 电台接口通常用 artists
                                if (arArray != null && arArray.length() > 0) {
                                    model.artist = arArray.getJSONObject(0).optString("name");
                                } else {
                                    model.artist = "未知主播";
                                }

                                JSONObject alObj = mainSong.optJSONObject("album");
                                model.coverUrl = alObj != null ? alObj.optString("picUrl") : programObj.optString("coverUrl");
                            } else {
                                // 兜底：如果有些纯电台没有 mainSong，直接取 mainTrackId
                                model.id = programObj.optString("mainTrackId");
                                model.name = programObj.optString("name");
                                model.coverUrl = programObj.optString("coverUrl");
                                JSONObject dj = programObj.optJSONObject("dj");
                                model.artist = dj != null ? dj.optString("nickname") : "未知主播";
                            }
                            trackList.add(model);
                        }
                        runOnUiThread(() -> trackAdapter.notifyDataSetChanged());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showToast("电台节目解析异常");
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) { showToast("获取电台失败"); }
        });
    }

    // 🌟 顺手修复：专门加载专辑的方法 (新碟上架点进去也会报错，用这个一并修了)
    private void loadAlbumTracks() {
        if (playlistId == null || playlistId.isEmpty()) return;
        HomeApiService homeApiService = RetrofitClient.getService(this, HomeApiService.class);
        homeApiService.getAlbumDetail(playlistId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    // 1. 获取专辑头部信息
                    JSONObject albumObj = jsonObject.optJSONObject("album");
                    if (albumObj != null) {
                        String realName = albumObj.optString("name");
                        String realCover = albumObj.optString("picUrl");
                        playlistName = realName;
                        playlistCover = realCover;
                        runOnUiThread(() -> {
                            if (tvTitle != null) tvTitle.setText(realName);
                            if (ivCover != null) Glide.with(PlaylistDetailActivity.this).load(realCover).into(ivCover);
                        });
                    }
                    // 2. 获取专辑歌曲
                    JSONArray songsArray = jsonObject.optJSONArray("songs");
                    if (songsArray != null && songsArray.length() > 0) {
                        trackList.clear();
                        for (int i = 0; i < songsArray.length(); i++) {
                            JSONObject trackObj = songsArray.getJSONObject(i);
                            PlaylistTrackModel model = new PlaylistTrackModel();
                            model.id = trackObj.optString("id");
                            model.name = trackObj.optString("name");
                            JSONArray arArray = trackObj.optJSONArray("ar");
                            model.artist = (arArray != null && arArray.length() > 0) ? arArray.getJSONObject(0).optString("name") : "未知歌手";
                            JSONObject alObj = trackObj.optJSONObject("al");
                            model.coverUrl = alObj != null ? alObj.optString("picUrl") : playlistCover;
                            trackList.add(model);
                        }
                        runOnUiThread(() -> trackAdapter.notifyDataSetChanged());
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }


    private void showToast(String msg) {
        runOnUiThread(() -> Toast.makeText(PlaylistDetailActivity.this, msg, Toast.LENGTH_SHORT).show());
    }

    // 内部歌曲实体模型
    private static class PlaylistTrackModel {
        String id;
        String name;
        String artist;
        String coverUrl;
    }

    // 内部类：列表适配器（复用 item_song_result 样式布局）
    private class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackViewHolder> {
        private List<PlaylistTrackModel> data;

        public TrackAdapter(List<PlaylistTrackModel> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // 🌟 强力复用已经写好的单行歌曲精美布局
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song_result, parent, false);
            return new TrackViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TrackViewHolder holder, int position) {
            PlaylistTrackModel track = data.get(position);
            holder.tvSongName.setText(track.name);
            holder.tvArtistName.setText(track.artist);

            // 🌟 核心功能：点击任意一首歌曲，打包全部参数传递给播放器页
            holder.itemView.setOnClickListener(v -> {
                // 1. 提取所有 ID
                java.util.ArrayList<String> currentPlaylistIds = new java.util.ArrayList<>();
                for (PlaylistTrackModel item : data) {
                    currentPlaylistIds.add(item.id);
                }

                // 🌟 2. [新增] 提取所有歌名
                java.util.ArrayList<String> playlistNames = new java.util.ArrayList<>();
                for (PlaylistTrackModel item : data) {
                    playlistNames.add(item.name);
                }

                Intent intent = new Intent(PlaylistDetailActivity.this, PlayerActivity.class);
                intent.putExtra("songId", track.id);
                intent.putExtra("songName", track.name);
                intent.putExtra("artistName", track.artist);
                intent.putExtra("coverUrl", track.coverUrl);

                intent.putStringArrayListExtra("playlistIds", currentPlaylistIds); // 传入 ID 列表
                intent.putStringArrayListExtra("playlistNames", playlistNames);    // 🌟 [新增] 传入歌名列表
                intent.putExtra("currentIndex", position);

                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class TrackViewHolder extends RecyclerView.ViewHolder {
            TextView tvSongName;
            TextView tvArtistName;

            public TrackViewHolder(@NonNull View itemView) {
                super(itemView);
                tvSongName = itemView.findViewById(R.id.tv_song_name);
                tvArtistName = itemView.findViewById(R.id.tv_artist_name);
            }
        }
    }
}