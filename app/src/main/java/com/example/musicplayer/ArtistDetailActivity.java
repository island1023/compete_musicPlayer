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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.util.Log;

public class ArtistDetailActivity extends AppCompatActivity {

    private String artistId;
    private String artistName;
    private String artistPic;

    private RecyclerView rvSongs;
    private ArtistSongAdapter adapter;
    private List<Map<String, String>> songList = new ArrayList<>();

    // 🌟 1. 在类顶部定义组件与状态变量
    private boolean isFavorited = false;
    private boolean isAnchor = false; // 用于判定是不是主播
    private TextView tvFavArtist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_detail);

        artistId = getIntent().getStringExtra("artistId");
        artistName = getIntent().getStringExtra("artistName");
        artistPic = getIntent().getStringExtra("artistPic");

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        TextView tvName = findViewById(R.id.tv_artist_name);
        tvName.setText(artistName);

        ImageView ivBg = findViewById(R.id.iv_artist_bg);
        if (artistPic != null && !artistPic.isEmpty()) {
            Glide.with(this).load(artistPic).into(ivBg);
        }

        rvSongs = findViewById(R.id.rv_artist_songs);
        rvSongs.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ArtistSongAdapter();
        rvSongs.setAdapter(adapter);

        // 🌟 2. 绑定在热门单曲旁的收藏视图，并依据数据库初始记录即时显现样式
        tvFavArtist = findViewById(R.id.tv_fav_artist);

        MusicDbHelper initialDb = new MusicDbHelper(this);
        if (initialDb.isFavArtist(artistId) || initialDb.isFavAnchor(artistId)) {
            isFavorited = true;
            tvFavArtist.setText("已收藏");
            tvFavArtist.setTextColor(android.graphics.Color.parseColor("#AAAAAA"));
        }
        initialDb.close();

        // 🌟 3. 关联双向持久化路由（按歌手/主播分流保存至对应表）
        tvFavArtist.setOnClickListener(v -> {
            isFavorited = !isFavorited;
            MusicDbHelper dbHelper = new MusicDbHelper(this);

            if (isAnchor) {
                dbHelper.toggleFavAnchor(artistId, artistName, artistPic, isFavorited);
            } else {
                dbHelper.toggleFavArtist(artistId, artistName, artistPic, isFavorited);
            }
            dbHelper.close();

            if (isFavorited) {
                tvFavArtist.setText("已收藏");
                tvFavArtist.setTextColor(android.graphics.Color.parseColor("#AAAAAA"));
                Toast.makeText(this, "收藏成功！", Toast.LENGTH_SHORT).show();
            } else {
                tvFavArtist.setText("收 藏");
                tvFavArtist.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
                Toast.makeText(this, "已取消收藏", Toast.LENGTH_SHORT).show();
            }
        });

        loadArtistSongs();
    }

    private void loadArtistSongs() {
        if (artistId == null || artistId.isEmpty()) return;

        UserApiService apiService = RetrofitClient.getService(this, UserApiService.class);
        apiService.getArtistDetail(artistId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                // 🌟 4. 拦截 404 异常：标记为主播态，切入专属的 /user/dj 数据接口
                if (response.code() == 404) {
                    isAnchor = true;
                    runOnUiThread(() -> {
                        Toast.makeText(ArtistDetailActivity.this, "检测到为主播，正在拉取电台列表...", Toast.LENGTH_SHORT).show();
                    });
                    fetchDjRadios(apiService, artistId);
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        JSONArray hotSongs = jsonObject.optJSONArray("hotSongs");

                        if (hotSongs != null && hotSongs.length() > 0) {
                            songList.clear();
                            for (int i = 0; i < hotSongs.length(); i++) {
                                JSONObject trackObj = hotSongs.getJSONObject(i);
                                Map<String, String> map = new HashMap<>();
                                map.put("songId", trackObj.optString("id"));
                                map.put("name", trackObj.optString("name"));

                                JSONArray arArray = trackObj.optJSONArray("ar");
                                if (arArray != null && arArray.length() > 0) {
                                    map.put("artist", arArray.getJSONObject(0).optString("name"));
                                } else {
                                    map.put("artist", artistName);
                                }

                                JSONObject alObj = trackObj.optJSONObject("al");
                                if (alObj != null) {
                                    map.put("coverUrl", alObj.optString("picUrl"));
                                }
                                songList.add(map);
                            }
                            runOnUiThread(() -> adapter.notifyDataSetChanged());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(ArtistDetailActivity.this, "网络请求失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchDjRadios(UserApiService apiService, String uid) {
        apiService.getUserDj(uid).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String bodyString = response.body().string();
                        JSONObject jsonObject = new JSONObject(bodyString);
                        JSONArray programs = jsonObject.optJSONArray("programs");

                        if (programs != null && programs.length() > 0) {
                            songList.clear();
                            for (int i = 0; i < programs.length(); i++) {
                                JSONObject programObj = programs.getJSONObject(i);
                                Map<String, String> map = new HashMap<>();

                                map.put("songId", programObj.optString("mainTrackId"));
                                map.put("name", programObj.optString("name"));
                                map.put("artist", programObj.optString("categoryName", "电台节目"));
                                map.put("coverUrl", programObj.optString("coverUrl"));
                                map.put("isRadio", "true");

                                songList.add(map);
                            }
                            runOnUiThread(() -> adapter.notifyDataSetChanged());
                        } else {
                            runOnUiThread(() -> Toast.makeText(ArtistDetailActivity.this, "该主播暂未发布电台节目", Toast.LENGTH_SHORT).show());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e("ArtistDetail", "解析主播电台异常: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                runOnUiThread(() -> Toast.makeText(ArtistDetailActivity.this, "获取电台失败", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private class ArtistSongAdapter extends RecyclerView.Adapter<ArtistSongAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song_result, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, String> song = songList.get(position);
            holder.tvSongName.setText(song.get("name"));
            holder.tvArtistName.setText(song.get("artist"));

            holder.itemView.setOnClickListener(v -> {
                if ("true".equals(song.get("isRadio"))) {
                    Toast.makeText(ArtistDetailActivity.this, "电台「" + song.get("name") + "」的节目单开发中...", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(ArtistDetailActivity.this, PlayerActivity.class);
                    intent.putExtra("songId", song.get("songId"));
                    intent.putExtra("songName", song.get("name"));
                    intent.putExtra("artistName", song.get("artist"));
                    intent.putExtra("coverUrl", song.get("coverUrl"));
                    startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return songList.size();
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