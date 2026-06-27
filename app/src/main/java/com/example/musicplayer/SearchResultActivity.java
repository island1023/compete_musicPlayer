package com.example.musicplayer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchResultActivity extends AppCompatActivity {

    private TextView tvBack;
    private TextView tvKeywordDisplay;
    private RecyclerView rvSearchResults;

    private SearchApiService apiService;
    private SongAdapter songAdapter;
    private List<ResultSongModel> songList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_result);

        // 1. 初始化视图控件
        tvBack = findViewById(R.id.tv_back);
        tvKeywordDisplay = findViewById(R.id.tv_keyword_display);
        rvSearchResults = findViewById(R.id.rv_search_results);

        // 2. 初始化网络请求服务
        apiService = RetrofitClient.getService(this, SearchApiService.class);

        // 3. 配置列表 RecyclerView
        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        songAdapter = new SongAdapter(songList);
        rvSearchResults.setAdapter(songAdapter);

        // 4. 获取传递过来的关键词
        String keyword = getIntent().getStringExtra("keyword");
        if (keyword != null && !keyword.isEmpty()) {
            tvKeywordDisplay.setText("“" + keyword + "” 的搜索结果");
            searchSongsFromApi(keyword); // 发起网络搜索
        } else {
            tvKeywordDisplay.setText("搜索结果");
            Toast.makeText(this, "未获取到搜索关键词", Toast.LENGTH_SHORT).show();
        }

        // 5. 返回按钮点击事件
        if (tvBack != null) {
            tvBack.setOnClickListener(v -> finish());
        }
    }

    // 异步请求后端/网易云API进行搜索
    private void searchSongsFromApi(String keyword) {
        apiService.searchSongs(keyword).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.body() != null) {
                        String jsonStr = response.body().string();
                        JSONObject jsonObject = new JSONObject(jsonStr);
                        JSONObject resultObj = jsonObject.optJSONObject("result");

                        if (resultObj != null) {
                            JSONArray songsArray = resultObj.optJSONArray("songs");
                            if (songsArray != null && songsArray.length() > 0) {
                                songList.clear();

                                for (int i = 0; i < songsArray.length(); i++) {
                                    JSONObject songObj = songsArray.getJSONObject(i);

                                    ResultSongModel model = new ResultSongModel();
                                    model.id = songObj.optString("id");
                                    model.name = songObj.optString("name");

                                    // 解析歌手名 (兼容 ar 和 artists 数据结构)
                                    JSONArray arArray = songObj.optJSONArray("ar");
                                    if (arArray == null) arArray = songObj.optJSONArray("artists");
                                    if (arArray != null && arArray.length() > 0) {
                                        model.artist = arArray.getJSONObject(0).optString("name");
                                    } else {
                                        model.artist = "未知歌手";
                                    }

                                    // 解析封面图 (兼容 al 和 album 结构)
                                    JSONObject alObj = songObj.optJSONObject("al");
                                    if (alObj == null) alObj = songObj.optJSONObject("album");
                                    if (alObj != null) {
                                        model.coverUrl = alObj.optString("picUrl");
                                    } else {
                                        model.coverUrl = "";
                                    }

                                    songList.add(model);
                                }

                                // 数据加载完成，刷新列表
                                songAdapter.notifyDataSetChanged();
                            } else {
                                Toast.makeText(SearchResultActivity.this, "未找到相关歌曲", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(SearchResultActivity.this, "数据解析异常", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(SearchResultActivity.this, "网络请求失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 内部类：歌曲数据实体模型
    private static class ResultSongModel {
        String id;
        String name;
        String artist;
        String coverUrl;
    }

    // 内部类：适配器 Adapter 负责渲染和点击事件
    private class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {
        private List<ResultSongModel> data;

        public SongAdapter(List<ResultSongModel> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song_result, parent, false);
            return new SongViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
            ResultSongModel song = data.get(position);
            holder.tvSongName.setText(song.name);
            holder.tvArtistName.setText(song.artist);

            // 点击条目：携带全部歌曲信息跳转去播放页面 (PlayerActivity)
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(SearchResultActivity.this, PlayerActivity.class);
                intent.putExtra("songId", song.id);
                intent.putExtra("songName", song.name);
                intent.putExtra("artistName", song.artist);
                intent.putExtra("coverUrl", song.coverUrl);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class SongViewHolder extends RecyclerView.ViewHolder {
            TextView tvSongName;
            TextView tvArtistName;

            public SongViewHolder(@NonNull View itemView) {
                super(itemView);
                tvSongName = itemView.findViewById(R.id.tv_song_name);
                tvArtistName = itemView.findViewById(R.id.tv_artist_name);
            }
        }
    }
}