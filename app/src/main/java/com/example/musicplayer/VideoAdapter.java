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

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private Context context;
    private List<VideoItem> videoList;

    public static class VideoItem {
        public String id;
        public String title;
        public String coverUrl;
        public String creatorName;

        // 新增
        public String url;


        public VideoItem(String id,
                         String title,
                         String coverUrl,
                         String creatorName,
                         String url) {

            this.id = id;
            this.title = title;
            this.coverUrl = coverUrl;
            this.creatorName = creatorName;
            this.url = url;
        }
    }

    public VideoAdapter(Context context, List<VideoItem> videoList) {
        this.context = context;
        this.videoList = videoList;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_video_card, parent, false);
        return new VideoViewHolder(view);
    }

   /* @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        VideoItem item = videoList.get(position);
        holder.tvTitle.setText(item.title);
        holder.tvCreator.setText(item.creatorName);

        if (item.coverUrl != null && !item.coverUrl.isEmpty()) {
            Glide.with(context).load(item.coverUrl).into(holder.ivCover);
        }


        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, VideoPlayerActivity.class);
            intent.putExtra("videoId", item.id);
            context.startActivity(intent);
        });
    }
*/

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {

        VideoItem item = videoList.get(position);

        holder.tvTitle.setText(item.title);

        // 正确字段
        holder.tvCreator.setText(item.creatorName);

        Glide.with(context)
                .load(item.coverUrl)
                .into(holder.ivCover);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, VideoPlayerActivity.class);
            intent.putExtra("videoId", item.id); // 只传 ID

            // 🌟 必须加上这三行，把数据传给 VideoPlayerActivity！
            intent.putExtra("title", item.title);
            intent.putExtra("creator", item.creatorName);
            intent.putExtra("coverUrl", item.coverUrl);

            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return videoList == null ? 0 : videoList.size();
    }

    static class VideoViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvTitle, tvCreator;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.iv_video_cover);
            tvTitle = itemView.findViewById(R.id.tv_video_title);
            tvCreator = itemView.findViewById(R.id.tv_video_creator);
        }
    }
}