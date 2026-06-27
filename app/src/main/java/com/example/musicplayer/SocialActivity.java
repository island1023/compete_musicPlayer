package com.example.musicplayer;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;

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

public class SocialActivity extends AppCompatActivity {

    private String uid;
    private UserApiService apiService;
    private RecyclerView rvSocial;
    private TextView tvEmptyState;
    private SocialAdapter adapter;
    private List<SocialUser> userList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_social);

        uid = getIntent().getStringExtra("uid");
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        tvEmptyState = findViewById(R.id.tv_empty_state);
        rvSocial = findViewById(R.id.rv_social);
        rvSocial.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SocialAdapter();
        rvSocial.setAdapter(adapter);

        apiService = new Retrofit.Builder().baseUrl("http://10.0.2.2:3000/")
                .addConverterFactory(GsonConverterFactory.create()).build().create(UserApiService.class);

        TabLayout tabLayout = findViewById(R.id.tab_social);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) loadData(true); // 关注
                else loadData(false); // 粉丝
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        loadData(true); // 默认加载关注列表
    }

    private void loadData(boolean isFollows) {
        userList.clear();
        adapter.notifyDataSetChanged();
        tvEmptyState.setVisibility(View.GONE);

        Call<ResponseBody> call = isFollows ? apiService.getUserFollows(uid, 30, 0) : apiService.getUserFolloweds(uid, 30, 0);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        String arrayKey = isFollows ? "follow" : "followeds";
                        JSONArray arr = json.optJSONArray(arrayKey);

                        if (arr == null || arr.length() == 0) {
                            tvEmptyState.setVisibility(View.VISIBLE);
                            tvEmptyState.setText(isFollows ? "还没有关注任何人" : "还没有粉丝");
                            return;
                        }

                        // 替换从 for 循环开始的部分
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);

                            // 🌟 核心修复点：强制修正关注状态
                            boolean isCurrentlyFollowed = isFollows ? true : obj.optBoolean("followed", false);

                            userList.add(new SocialUser(
                                    obj.getString("userId"),
                                    obj.getString("nickname"),
                                    obj.getString("avatarUrl"),
                                    isCurrentlyFollowed
                            ));
                        }
                        adapter.notifyDataSetChanged();
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }

    // 内部数据类和 Adapter
    private class SocialUser {
        String id, name, avatarUrl; boolean isFollowed;
        SocialUser(String id, String name, String avatarUrl, boolean isFollowed) {
            this.id = id; this.name = name; this.avatarUrl = avatarUrl; this.isFollowed = isFollowed;
        }
    }

    private class SocialAdapter extends RecyclerView.Adapter<SocialAdapter.VH> {
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_social, parent, false));
        }

        @Override public void onBindViewHolder(@NonNull VH holder, int position) {
            SocialUser user = userList.get(position);
            holder.tvName.setText(user.name);
            Glide.with(SocialActivity.this).load(user.avatarUrl).circleCrop().into(holder.ivAvatar);

            // 渲染按钮状态
            if (user.isFollowed) {
                holder.btnFollow.setText("取消关注");
                holder.btnFollow.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#333333")));
            } else {
                holder.btnFollow.setText("关 注");
                holder.btnFollow.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.RED));
            }

            // 点击关注/取消关注
            holder.btnFollow.setOnClickListener(v -> {
                // 暂时禁用按钮，防止连点
                holder.btnFollow.setEnabled(false);

                int t = user.isFollowed ? 0 : 1; // 1为关注，0为取消
                apiService.followUser(user.id, t).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        holder.btnFollow.setEnabled(true);
                        if (response.isSuccessful()) {
                            user.isFollowed = !user.isFollowed;

                            // 🌟 核心修复点：动态获取当前 ViewHolder 的最新位置
                            int currentPos = holder.getAdapterPosition();
                            // 确保该 Item 没有被移出屏幕或销毁
                            if (currentPos != RecyclerView.NO_POSITION) {
                                notifyItemChanged(currentPos);
                            }

                            Toast.makeText(SocialActivity.this, user.isFollowed ? "已关注" : "已取消关注", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(SocialActivity.this, "操作失败，请稍后再试", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override public void onFailure(Call<ResponseBody> call, Throwable t) {
                        holder.btnFollow.setEnabled(true);
                        Toast.makeText(SocialActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }

        @Override public int getItemCount() { return userList.size(); }

        class VH extends RecyclerView.ViewHolder {
            ImageView ivAvatar; TextView tvName, btnFollow;
            public VH(View v) {
                super(v);
                ivAvatar = v.findViewById(R.id.iv_social_avatar);
                tvName = v.findViewById(R.id.tv_social_name);
                btnFollow = v.findViewById(R.id.btn_follow);
            }
        }
    }
}