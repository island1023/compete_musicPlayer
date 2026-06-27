package com.example.musicplayer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
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
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import android.content.Intent;
public class StylePlaylistActivity extends AppCompatActivity {

    private int tagId;
    private String tagName;
    private RecyclerView rvPlaylists;
    private DiscoverApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_style_playlist);

        // 获取传过来的曲风数据
        tagId = getIntent().getIntExtra("tagId", 0);
        tagName = getIntent().getStringExtra("tagName");

        TextView tvTitle = findViewById(R.id.tv_title);
        tvTitle.setText(tagName != null ? tagName + "歌单" : "曲风歌单");
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        rvPlaylists = findViewById(R.id.rv_playlists);
        rvPlaylists.setLayoutManager(new GridLayoutManager(this, 2)); // 两列网格展示

        initRetrofit();
        loadPlaylists();
    }

    private void initRetrofit() {
        apiService = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:3000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(DiscoverApiService.class);
    }

    private void loadPlaylists() {
        // 🌟 核心修复 1：使用 tagName (如 "摇滚", "华语") 而不是 tagId 进行请求
        // 注意：新接口控制数量的参数名是 limit
        apiService.getPlaylistsByTag(tagName, 30).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());

                        // 🌟 核心修复 2：/top/playlist 接口没有 "data" 层
                        // 包含歌单的数组直接在根目录下，名字叫 "playlists" (复数)
                        JSONArray playlistArray = jsonObject.optJSONArray("playlists");

                        if (playlistArray != null && playlistArray.length() > 0) {
                            List<PlaylistItem> items = new ArrayList<>();

                            for (int i = 0; i < playlistArray.length(); i++) {
                                JSONObject pl = playlistArray.getJSONObject(i);
                                items.add(new PlaylistItem(
                                        pl.optString("id"),
                                        pl.optString("name"),
                                        pl.optString("coverImgUrl")
                                ));
                            }

                            // 刷新列表
                            runOnUiThread(() -> {
                                rvPlaylists.setAdapter(new PlaylistAdapter(items));
                            });
                        } else {
                            runOnUiThread(() -> {
                                Toast.makeText(StylePlaylistActivity.this, "该曲风下暂无歌单", Toast.LENGTH_SHORT).show();
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            Toast.makeText(StylePlaylistActivity.this, "数据解析异常", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                runOnUiThread(() -> {
                    Toast.makeText(StylePlaylistActivity.this, "网络请求失败", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // 内部数据模型
    private static class PlaylistItem {
        String id; String name; String coverUrl;
        public PlaylistItem(String id, String name, String coverUrl) {
            this.id = id; this.name = name; this.coverUrl = coverUrl;
        }
    }

    // 内部适配器
    private class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.VH> {
        private List<PlaylistItem> data;
        public PlaylistAdapter(List<PlaylistItem> data) { this.data = data; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist_grid, parent, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            PlaylistItem item = data.get(position);
            holder.tvName.setText(item.name);
            Glide.with(StylePlaylistActivity.this).load(item.coverUrl).into(holder.ivCover);

            // 🌟 核心修复：点击歌单事件，从弹出 Toast 修改为跳转至歌单详情页
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(StylePlaylistActivity.this, PlaylistDetailActivity.class);
                intent.putExtra("playlistId", item.id);
                intent.putExtra("playlistName", item.name);
                intent.putExtra("playlistCover", item.coverUrl);
                startActivity(intent);
            });
        }

        @Override public int getItemCount() { return data.size(); }

        class VH extends RecyclerView.ViewHolder {
            ImageView ivCover; TextView tvName;
            public VH(View v) {
                super(v);
                ivCover = v.findViewById(R.id.iv_cover);
                tvName = v.findViewById(R.id.tv_playlist_name);
            }
        }
    }
}