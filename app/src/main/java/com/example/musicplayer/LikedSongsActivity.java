package com.example.musicplayer;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
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

public class LikedSongsActivity extends AppCompatActivity {
    private ListView lvLikedSongs;
    private UserApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_liked_songs);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        lvLikedSongs = findViewById(R.id.lv_liked_songs);
        // 使用带有 Cookie 拦截器的 RetrofitClient
        apiService = RetrofitClient.getService(this, UserApiService.class);

        loadLikedSongs();
    }

    private void loadLikedSongs() {
        MusicDbHelper dbHelper = new MusicDbHelper(this);
        // 这里的变量名叫 dataList，不能写成 list
        List<Map<String, String>> dataList = dbHelper.getLikedSongs();

        if (dataList != null && !dataList.isEmpty()) {
            SimpleAdapter adapter = new SimpleAdapter(
                    this, dataList,
                    android.R.layout.simple_list_item_2,
                    new String[]{"title", "artist"},
                    new int[]{android.R.id.text1, android.R.id.text2}
            );
            lvLikedSongs.setAdapter(adapter);

            // 🌟 修复点：下面所有的循环和获取，都统一改用 dataList
            lvLikedSongs.setOnItemClickListener((parent, view, position, id) -> {
                Map<String, String> clickedSong = dataList.get(position);

                java.util.ArrayList<String> playlistIds = new java.util.ArrayList<>();
                for (Map<String, String> item : dataList) {
                    playlistIds.add(item.get("songId"));
                }

                android.content.Intent intent = new android.content.Intent(LikedSongsActivity.this, PlayerActivity.class);
                intent.putExtra("songId", clickedSong.get("songId"));
                intent.putExtra("songName", clickedSong.get("title"));
                intent.putExtra("artistName", clickedSong.get("artist"));
                intent.putExtra("coverUrl", clickedSong.get("coverUrl"));
                intent.putStringArrayListExtra("playlistIds", playlistIds);
                intent.putExtra("currentIndex", position);

                startActivity(intent);
            });
        } else {
            Toast.makeText(this, "暂无喜欢的音乐", Toast.LENGTH_SHORT).show();
        }
    }

    // 在 fetchSongDetails 方法中
    private void fetchSongDetails(String ids) {
        apiService.getSongDetail(ids).enqueue(new Callback<ResponseBody>() {
            @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.body() == null) return;
                    JSONObject json = new JSONObject(response.body().string());

                    // 🌟 改为 optJSONArray，并判断是否为空
                    JSONArray songs = json.optJSONArray("songs");
                    if (songs == null) {
                        android.util.Log.e("DEBUG", "接口未返回 songs 数组: " + json.toString());
                        return;
                    }

                    List<Map<String, String>> list = new ArrayList<>();
                    for (int i = 0; i < songs.length(); i++) {
                        JSONObject song = songs.getJSONObject(i);
                        Map<String, String> map = new HashMap<>();
                        map.put("title", song.optString("name", "未知"));
                        map.put("artist", song.getJSONArray("ar").getJSONObject(0).optString("name", "未知"));
                        list.add(map);
                    }
                    SimpleAdapter adapter = new SimpleAdapter(LikedSongsActivity.this, list,
                            android.R.layout.simple_list_item_2, new String[]{"title", "artist"},
                            new int[]{android.R.id.text1, android.R.id.text2});
                    runOnUiThread(() -> lvLikedSongs.setAdapter(adapter));
                } catch (Exception e) { e.printStackTrace(); }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }
}