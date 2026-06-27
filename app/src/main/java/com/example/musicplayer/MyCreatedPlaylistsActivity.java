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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;
import java.util.Map;

public class MyCreatedPlaylistsActivity extends AppCompatActivity {

    private RecyclerView rvMyPlaylists;
    private CreatedPlaylistAdapter adapter;
    private List<Map<String, String>> localPlaylists;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_created_playlists);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        rvMyPlaylists = findViewById(R.id.rv_my_playlists);
        rvMyPlaylists.setLayoutManager(new GridLayoutManager(this, 2)); // 双列大框

        loadCreatedPlaylists();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCreatedPlaylists();
    }

    private void loadCreatedPlaylists() {
        MusicDbHelper dbHelper = new MusicDbHelper(this);
        // 🌟 修复：调用正确的本地歌单获取方法
        localPlaylists = dbHelper.getLocalPlaylists();
        dbHelper.close();

        if (adapter == null) {
            adapter = new CreatedPlaylistAdapter();
            rvMyPlaylists.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    private class CreatedPlaylistAdapter extends RecyclerView.Adapter<CreatedPlaylistAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fav_playlist, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, String> playlist = localPlaylists.get(position);
            String name = playlist.get("name");
            if (name == null) name = playlist.get("title");

            String coverUrl = playlist.get("coverUrl");
            if (coverUrl == null) coverUrl = playlist.get("cover");

            String pid = playlist.get("pid");
            if (pid == null) pid = playlist.get("id");
            // 🌟 修复：兼容 MusicDbHelper 中 getLocalPlaylists 方法将 pid 映射为 songId 的情况
            if (pid == null) pid = playlist.get("songId");

            holder.tvName.setText(name);
            Glide.with(MyCreatedPlaylistsActivity.this)
                    .load(coverUrl)
                    .placeholder(R.drawable.ic_logo)
                    .error(R.drawable.ic_logo)
                    .into(holder.ivCover);

            String finalPid = pid;
            String finalName = name;
            String finalCoverUrl = coverUrl;
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(MyCreatedPlaylistsActivity.this, MyPlaylistDetailActivity.class);
                intent.putExtra("playlistId", finalPid);
                intent.putExtra("playlistName", finalName);
                intent.putExtra("playlistCover", finalCoverUrl);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return localPlaylists == null ? 0 : localPlaylists.size();
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