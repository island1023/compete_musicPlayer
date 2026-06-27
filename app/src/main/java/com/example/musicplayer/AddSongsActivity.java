package com.example.musicplayer;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
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
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AddSongsActivity extends AppCompatActivity {

    private String pid; // 从上个页面传过来的歌单 ID
    private EditText etSearchKeyword;
    private RecyclerView rvSearchResults;
    private UserApiService apiService;
    private SearchSongAdapter adapter;
    private List<SongItem> songList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_songs);

        // 获取传过来的歌单 ID
        pid = getIntent().getStringExtra("pid");

        initViews();
        initRetrofit();
        setupListeners();
    }

    private void initViews() {
        etSearchKeyword = findViewById(R.id.et_search_keyword);
        rvSearchResults = findViewById(R.id.rv_search_results);

        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SearchSongAdapter();
        rvSearchResults.setAdapter(adapter);
    }

    private void initRetrofit() {
        apiService = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:3000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(UserApiService.class);
    }

    private void setupListeners() {
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        Button btnSearch = findViewById(R.id.btn_search);
        btnSearch.setOnClickListener(v -> performSearch());

        // 监听软键盘的“搜索”回车键
        etSearchKeyword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                performSearch();
                return true;
            }
            return false;
        });
    }

    // 执行搜索
    private void performSearch() {
        String keyword = etSearchKeyword.getText().toString().trim();
        if (TextUtils.isEmpty(keyword)) {
            Toast.makeText(this, "请输入搜索关键词", Toast.LENGTH_SHORT).show();
            return;
        }

        // 隐藏软键盘
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etSearchKeyword.getWindowToken(), 0);

        songList.clear();
        adapter.notifyDataSetChanged();
        Toast.makeText(this, "正在搜索...", Toast.LENGTH_SHORT).show();

        // 调用 /cloudsearch 接口，type=1 表示单曲
        apiService.searchMusic(keyword, 30, 0, 1).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        JSONObject result = json.optJSONObject("result");
                        if (result != null) {
                            JSONArray songs = result.optJSONArray("songs");
                            if (songs != null && songs.length() > 0) {
                                for (int i = 0; i < songs.length(); i++) {
                                    JSONObject songObj = songs.getJSONObject(i);
                                    String id = songObj.getString("id");
                                    String name = songObj.getString("name");
                                    String artist = songObj.getJSONArray("ar").getJSONObject(0).getString("name");
                                    songList.add(new SongItem(id, name, artist));
                                }
                                runOnUiThread(() -> adapter.notifyDataSetChanged());
                                return;
                            }
                        }
                        Toast.makeText(AddSongsActivity.this, "未搜索到相关歌曲", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(AddSongsActivity.this, "解析失败", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(AddSongsActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 添加歌曲到歌单的核心逻辑
    private void addTrackToPlaylist(SongItem song, int position, TextView btnAdd) {
        if (TextUtils.isEmpty(pid)) {
            Toast.makeText(this, "歌单ID获取失败", Toast.LENGTH_SHORT).show();
            return;
        }

        // 网易云要求带上时间戳防止缓存
        long timestamp = System.currentTimeMillis();

        apiService.operatePlaylistTracks("add", pid, song.id, timestamp).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    String resStr = response.body().string();
                    JSONObject json = new JSONObject(resStr);
                    // 网易云返回的状态码可能是 body 里面的 code，或者是外层的 code
                    int code = json.has("body") ? json.getJSONObject("body").optInt("code", 0) : json.optInt("code", 0);

                    if (code == 200) {
                        Toast.makeText(AddSongsActivity.this, "已添加到歌单", Toast.LENGTH_SHORT).show();
                        // 改变按钮状态，防止重复添加
                        runOnUiThread(() -> {
                            song.isAdded = true;
                            btnAdd.setText("已添加");
                            btnAdd.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#333333")));
                        });
                    } else if (code == 502) {
                        Toast.makeText(AddSongsActivity.this, "歌曲已存在或歌单受限(502)", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(AddSongsActivity.this, "添加失败: " + code, Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(AddSongsActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- 数据类 ---
    private static class SongItem {
        String id, name, artist;
        boolean isAdded = false; // 记录是否已被添加

        SongItem(String id, String name, String artist) {
            this.id = id; this.name = name; this.artist = artist;
        }
    }

    // --- 适配器 ---
    private class SearchSongAdapter extends RecyclerView.Adapter<SearchSongAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_add_song, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SongItem song = songList.get(position);
            holder.tvName.setText(song.name);
            holder.tvArtist.setText(song.artist);

            if (song.isAdded) {
                holder.btnAdd.setText("已添加");
                holder.btnAdd.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#333333")));
            } else {
                holder.btnAdd.setText("＋ 添加");
                holder.btnAdd.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.RED));
            }

            holder.btnAdd.setOnClickListener(v -> {
                if (!song.isAdded) {
                    addTrackToPlaylist(song, position, holder.btnAdd);
                }
            });
        }

        @Override
        public int getItemCount() { return songList.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvArtist, btnAdd;
            ViewHolder(View view) {
                super(view);
                tvName = view.findViewById(R.id.tv_song_name);
                tvArtist = view.findViewById(R.id.tv_artist_name);
                btnAdd = view.findViewById(R.id.btn_add_to_playlist);
            }
        }
    }
}