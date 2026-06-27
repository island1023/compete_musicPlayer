package com.example.musicplayer;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;
import java.util.Map;

public class CommonRecordAdapter extends RecyclerView.Adapter<CommonRecordAdapter.ViewHolder> {
    private List<Map<String, String>> dataList;
    private Context context;
    private int type; // 1: 歌手, 2: 主播, 3: MV

    public CommonRecordAdapter(List<Map<String, String>> dataList, Context context, int type) {
        this.dataList = dataList;
        this.context = context;
        this.type = type;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_common_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, String> item = dataList.get(position);

        if (type == 1 || type == 2) {
            holder.tvMain.setText(item.get("name"));
            holder.tvSub.setText(type == 1 ? "🎤 歌手" : "🎙️ 播客");
            Glide.with(context).load(item.get("picUrl")).circleCrop().into(holder.ivCover);

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, ArtistDetailActivity.class);
                intent.putExtra("artistId", item.get("id"));
                intent.putExtra("artistName", item.get("name"));
                intent.putExtra("artistPic", item.get("picUrl"));
                context.startActivity(intent);
            });

        } else if (type == 3) {
            holder.tvMain.setText(item.get("title"));
            holder.tvSub.setText(item.get("creator"));

            // 🌟 修复黑屏问题：增加 placeholder 以免 URL 为空或请求失败时显示黑块
            String cover = item.get("coverUrl");
            if (cover != null && !cover.isEmpty()) {
                Glide.with(context)
                        .load(cover)
                        .placeholder(R.drawable.ic_launcher_background) // 请替换为你项目中有的占位图
                        .into(holder.ivCover);
            } else {
                holder.ivCover.setImageResource(R.drawable.ic_launcher_background); // 设置一个默认图
            }

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, VideoPlayerActivity.class);
                intent.putExtra("videoId", item.get("videoId"));
                intent.putExtra("title", item.get("title"));
                intent.putExtra("creator", item.get("creator"));
                intent.putExtra("coverUrl", item.get("coverUrl"));
                context.startActivity(intent);
            });
        }
    }

    @Override
    public int getItemCount() { return dataList.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvMain, tvSub;
        ViewHolder(View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.iv_cover);
            tvMain = itemView.findViewById(R.id.tv_main);
            tvSub = itemView.findViewById(R.id.tv_sub);
        }
    }
}