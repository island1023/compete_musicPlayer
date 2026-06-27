package com.example.musicplayer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;
import java.util.Map;
import androidx.recyclerview.widget.GridLayoutManager;

public class FavPlaylistsActivity extends AppCompatActivity {

    private RecyclerView rvFavPlaylists;
    private FavPlaylistAdapter adapter;
    private List<Map<String, String>> favList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fav_playlists);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        rvFavPlaylists = findViewById(R.id.rv_fav_playlists);
        rvFavPlaylists.setLayoutManager(new GridLayoutManager(this, 2)); // 🌟 变更为双列大方框

        loadFavPlaylists();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFavPlaylists(); // 每次回到页面时刷新，防止在详情页取消收藏后这里不更新
    }

    private void loadFavPlaylists() {
        MusicDbHelper dbHelper = new MusicDbHelper(this);
        favList = dbHelper.getFavPlaylists();
        dbHelper.close();

        if (adapter == null) {
            adapter = new FavPlaylistAdapter();
            rvFavPlaylists.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    private class FavPlaylistAdapter extends RecyclerView.Adapter<FavPlaylistAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fav_playlist, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, String> playlist = favList.get(position);

            // 🌟 多重 Key 安全校验：彻底解决因底层键值不一致导致的名称封面空白
            String name = playlist.get("name");
            if (name == null) name = playlist.get("title");
            if (name == null) name = playlist.get("playlistName");
            if (name == null) name = "未命名歌单";

            String coverUrl = playlist.get("coverUrl");
            if (coverUrl == null) coverUrl = playlist.get("cover");
            if (coverUrl == null) coverUrl = playlist.get("coverImgUrl");
            if (coverUrl == null) coverUrl = playlist.get("playlistCover");

            String pid = playlist.get("pid");
            if (pid == null) pid = playlist.get("id");

            holder.tvName.setText(name);
            Glide.with(FavPlaylistsActivity.this)
                    .load(coverUrl)
                    .placeholder(R.drawable.ic_logo)
                    .error(R.drawable.ic_logo)
                    .into(holder.ivCover);

            String finalPid = pid;
            String finalName = name;
            String finalCoverUrl = coverUrl;
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(FavPlaylistsActivity.this, PlaylistDetailActivity.class);
                intent.putExtra("playlistId", finalPid);
                intent.putExtra("playlistName", finalName);
                intent.putExtra("playlistCover", finalCoverUrl);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return favList == null ? 0 : favList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivCover;
            TextView tvName;

            ViewHolder(View itemView) {
                super(itemView);
                ivCover = itemView.findViewById(R.id.iv_cover);
                tvName = itemView.findViewById(R.id.tv_name);
            }
        }
    }
}