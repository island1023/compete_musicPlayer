package com.example.musicplayer;

import android.os.Bundle;
import android.widget.ImageView;
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

public class RecentActivity extends AppCompatActivity {
    private ListView lvRecentSongs;
    private UserApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recent);

        lvRecentSongs = findViewById(R.id.lv_recent_songs);
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        apiService = RetrofitClient.getService(this, UserApiService.class);

        loadRecentSongs();
    }

    private void loadRecentSongs() {
        MusicDbHelper dbHelper = new MusicDbHelper(this);
        // 这里的变量名叫 dataList
        List<Map<String, String>> dataList = dbHelper.getRecentSongs();

        if (dataList != null && !dataList.isEmpty()) {
            SimpleAdapter adapter = new SimpleAdapter(
                    this, dataList,
                    android.R.layout.simple_list_item_2,
                    new String[]{"title", "artist"},
                    new int[]{android.R.id.text1, android.R.id.text2}
            );
            lvRecentSongs.setAdapter(adapter);

            // 🌟 修复点：这里必须用 lvRecentSongs 和 dataList，且上下文必须是 RecentActivity.this
            lvRecentSongs.setOnItemClickListener((parent, view, position, id) -> {
                Map<String, String> clickedSong = dataList.get(position);

                java.util.ArrayList<String> playlistIds = new java.util.ArrayList<>();
                for (Map<String, String> item : dataList) {
                    playlistIds.add(item.get("songId"));
                }

                android.content.Intent intent = new android.content.Intent(RecentActivity.this, PlayerActivity.class);
                intent.putExtra("songId", clickedSong.get("songId"));
                intent.putExtra("songName", clickedSong.get("title"));
                intent.putExtra("artistName", clickedSong.get("artist"));
                intent.putExtra("coverUrl", clickedSong.get("coverUrl"));
                intent.putStringArrayListExtra("playlistIds", playlistIds);
                intent.putExtra("currentIndex", position);

                startActivity(intent);
            });
        } else {
            Toast.makeText(this, "暂无本地最近播放记录", Toast.LENGTH_SHORT).show();
        }
    }
}