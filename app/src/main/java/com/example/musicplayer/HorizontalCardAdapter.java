package com.example.musicplayer;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

public class HorizontalCardAdapter extends RecyclerView.Adapter<HorizontalCardAdapter.ViewHolder> {

    // 定义极其丰富的卡片类型
    public static final int TYPE_PLAYLIST = 1; // 歌单
    public static final int TYPE_ALBUM = 2;    // 专辑
    public static final int TYPE_RADIO = 3;    // 电台
    public static final int TYPE_SONG = 4;     // 单曲
    public static final int TYPE_ARTIST = 5;   // 歌手
    public static final int TYPE_TOPLIST = 6;  // 榜单

    private Context context;
    private List<CardItem> itemList;

    public HorizontalCardAdapter(Context context, List<CardItem> itemList) {
        this.context = context;
        this.itemList = itemList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_horizontal_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CardItem item = itemList.get(position);
        holder.tvName.setText(item.name);

        // 🌟 核心优化：强制获取 140x140 的缩略图，体积缩小 90%，瞬间丝滑！
        // 注意：网易云接口返回的图片链接通常是干净的，直接拼接即可
        String optimizedPicUrl = item.picUrl + "?param=140y140";

        // 加载圆角封面图 (使用优化后的 URL)
        Glide.with(context)
                .load(optimizedPicUrl)
                .apply(RequestOptions.bitmapTransform(new RoundedCorners(24)))
                .into(holder.ivCover);

        // 🌟 核心：统一的点击跳转路由分发
        holder.itemView.setOnClickListener(v -> {
            Intent detailIntent = new Intent(context, PlaylistDetailActivity.class);
            switch (item.itemType) {
                case TYPE_PLAYLIST:
                    detailIntent.putExtra("playlistId", item.id);
                    detailIntent.putExtra("type", "playlist");
                    detailIntent.putExtra("playlistName", item.name); // 🌟 新增：提前传歌单名
                    detailIntent.putExtra("playlistCover", optimizedPicUrl); // 🌟 新增：提前传封面
                    context.startActivity(detailIntent);
                    break;
                case TYPE_ALBUM:
                    detailIntent.putExtra("playlistId", item.id);
                    detailIntent.putExtra("type", "album");
                    detailIntent.putExtra("playlistName", item.name); // 🌟 新增
                    detailIntent.putExtra("playlistCover", optimizedPicUrl); // 🌟 新增
                    context.startActivity(detailIntent);
                    break;
                case TYPE_RADIO:
                    detailIntent.putExtra("playlistId", item.id);
                    detailIntent.putExtra("type", "radio");
                    detailIntent.putExtra("playlistName", item.name); // 🌟 新增
                    detailIntent.putExtra("playlistCover", optimizedPicUrl); // 🌟 新增
                    context.startActivity(detailIntent);
                    break;
                // 在 HorizontalCardAdapter.java 中修改 TYPE_SONG 的 case
                case TYPE_SONG:
                    // 每日推歌/新音乐不需要跳转 PlaylistDetailActivity，因为没有对应的 ID 接口
                    // 直接跳转播放器开始播放，或者把整串列表数据传过去
                    Intent playerIntent = new Intent(context, PlayerActivity.class);
                    playerIntent.putExtra("songId", item.id);
                    playerIntent.putExtra("songName", item.name);
                    playerIntent.putExtra("artistName", "点击播放"); // 简易处理
                    playerIntent.putExtra("coverUrl", item.picUrl);
                    context.startActivity(playerIntent);
                    break;

                case TYPE_ARTIST:
                    // 🌟 修改：正式跳转到歌手详情页
                    Intent artistIntent = new Intent(context, ArtistDetailActivity.class);
                    artistIntent.putExtra("artistId", item.id);
                    artistIntent.putExtra("artistName", item.name);
                    artistIntent.putExtra("artistPic", item.picUrl);
                    context.startActivity(artistIntent);
                    break;
                case TYPE_TOPLIST:
                    detailIntent.putExtra("playlistId", item.id);
                    detailIntent.putExtra("type", "playlist"); // 榜单本质上也是歌单
                    context.startActivity(detailIntent);
                    break;
            }
        });
    }

    @Override
    public int getItemCount() {
        return itemList != null ? itemList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.iv_cover);
            tvName = itemView.findViewById(R.id.tv_title);
        }
    }

    public static class CardItem {
        public String id;
        public String picUrl;
        public String name;
        public int itemType;

        public CardItem(String id, String picUrl, String name, int itemType) {
            this.id = id;
            this.picUrl = picUrl;
            this.name = name;
            this.itemType = itemType;
        }
    }
}