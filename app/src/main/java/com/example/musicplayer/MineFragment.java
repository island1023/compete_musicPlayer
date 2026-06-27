package com.example.musicplayer;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MineFragment extends Fragment {

    private ImageView ivAvatar;
    private TextView tvNickname, tvLevel, tvFollows, tvFans;
    private TabLayout tabLayout;

    // 🌟 1. 将类头部的 layoutPodcast 改名为 layoutArtistMv
    private LinearLayout layoutMusic, layoutArtistMv, layoutNotes;

    private String currentUid;
    private UserApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mine, container, false);
        currentUid = SpUtils.getUserId(requireContext());
        initViews(view);
        initRetrofit();
        setupListeners(view);
        setupTabs();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!currentUid.isEmpty()) fetchUserInfo();
    }

    private void initViews(View view) {
        ivAvatar = view.findViewById(R.id.iv_avatar);
        tvNickname = view.findViewById(R.id.tv_nickname);
        tvLevel = view.findViewById(R.id.tv_level);
        tvFollows = view.findViewById(R.id.tv_follows);
        tvFans = view.findViewById(R.id.tv_fans);

        tabLayout = view.findViewById(R.id.tab_layout);
        layoutMusic = view.findViewById(R.id.layout_music);

        // 🌟 2. 在 initViews 中更新绑定为新布局 id
        layoutArtistMv = view.findViewById(R.id.layout_artist_mv);
        layoutNotes = view.findViewById(R.id.layout_notes);
    }

    private void initRetrofit() {
        apiService = new Retrofit.Builder().baseUrl("http://10.0.2.2:3000/")
                .addConverterFactory(GsonConverterFactory.create()).build().create(UserApiService.class);
    }

    private void setupListeners(View view) {
        View.OnClickListener profileClickListener = v -> startActivity(new Intent(getActivity(), ProfileActivity.class));
        ivAvatar.setOnClickListener(profileClickListener);
        tvNickname.setOnClickListener(profileClickListener);

        view.findViewById(R.id.ll_social_stats).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SocialActivity.class);
            intent.putExtra("uid", currentUid);
            startActivity(intent);
        });

        view.findViewById(R.id.btn_recent).setOnClickListener(v -> startActivity(new Intent(getActivity(), RecentActivity.class)));
        view.findViewById(R.id.btn_local).setOnClickListener(v -> startActivity(new Intent(getActivity(), ImportLocalActivity.class)));

        view.findViewById(R.id.tv_fav_playlists).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), FavPlaylistsActivity.class));
        });

        view.findViewById(R.id.tv_create_playlist).setOnClickListener(v -> showCreatePlaylistDialog());
        view.findViewById(R.id.tv_liked_songs).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), LikedSongsActivity.class);
            intent.putExtra("uid", currentUid);
            startActivity(intent);
        });

        view.findViewById(R.id.tv_my_created_playlists).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), MyCreatedPlaylistsActivity.class);
            startActivity(intent);
        });

        view.findViewById(R.id.tv_import_local).setOnClickListener(v -> startActivity(new Intent(getActivity(), ImportLocalActivity.class)));

        // 🌟 4. 在 setupListeners 中给新增的三个选项加上点击事件绑定
        // 把这三行 Toast 替换掉：
        view.findViewById(R.id.tv_fav_artists).setOnClickListener(v -> startActivity(new Intent(getActivity(), FavArtistsActivity.class)));
        view.findViewById(R.id.tv_fav_anchors).setOnClickListener(v -> startActivity(new Intent(getActivity(), FavAnchorsActivity.class)));
        view.findViewById(R.id.tv_mv_history).setOnClickListener(v -> startActivity(new Intent(getActivity(), MvHistoryActivity.class)));
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                layoutMusic.setVisibility(View.GONE);
                layoutArtistMv.setVisibility(View.GONE);
                layoutNotes.setVisibility(View.GONE);

                // 🌟 3. 在 setupTabs 中修改切换与渲染逻辑，去除 fetchUserDj() 的冗余网络请求
                if (tab.getPosition() == 0) {
                    layoutMusic.setVisibility(View.VISIBLE);
                } else if (tab.getPosition() == 1) {
                    layoutArtistMv.setVisibility(View.VISIBLE);
                } else if (tab.getPosition() == 2) {
                    layoutNotes.setVisibility(View.VISIBLE);
                    fetchUserEvent(); // 拉取笔记真实数据
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void fetchUserInfo() {
        try {
            MusicDbHelper dbHelper = new MusicDbHelper(requireContext());
            android.database.Cursor cursor = dbHelper.getUserProfile(currentUid);
            if (cursor != null && cursor.moveToFirst()) {
                String localNick = cursor.getString(cursor.getColumnIndexOrThrow("nickname"));
                String localAvatarPath = cursor.getString(cursor.getColumnIndexOrThrow("avatarPath"));

                if (localNick != null && !localNick.isEmpty()) {
                    tvNickname.setText(localNick);
                }
                if (localAvatarPath != null && !localAvatarPath.isEmpty()) {
                    Glide.with(requireContext())
                            .load(localAvatarPath)
                            .placeholder(R.drawable.ic_logo)
                            .error(R.drawable.ic_logo)
                            .circleCrop()
                            .into(ivAvatar);
                }
            }
            if (cursor != null) cursor.close();
            dbHelper.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        apiService.getUserDetail(currentUid).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null && isAdded()) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        int level = jsonObject.optInt("level", 0);
                        JSONObject profile = jsonObject.getJSONObject("profile");

                        /*
                        requireActivity().runOnUiThread(() -> {
                            tvLevel.setText("Lv." + level);
                            tvFollows.setText(profile.optInt("follows") + " 关注");
                            tvFans.setText(profile.optInt("followeds") + " 粉丝");

                            MusicDbHelper db = new MusicDbHelper(requireContext());
                            android.database.Cursor c = db.getUserProfile(currentUid);
                            boolean hasLocalCache = c != null && c.moveToFirst();
                            if (c != null) c.close();
                            db.close();

                            if (!hasLocalCache) {
                                tvNickname.setText(profile.optString("nickname"));
                                String avatarUrl = profile.optString("avatarUrl");
                                if (!avatarUrl.isEmpty()) {
                                    Glide.with(requireContext()).load(avatarUrl).placeholder(R.drawable.ic_logo).circleCrop().into(ivAvatar);
                                }
                            }
                        });
                        */
                        requireActivity().runOnUiThread(() -> {
                            tvLevel.setText("Lv." + level);
                            tvFollows.setText(profile.optInt("follows") + " 关注");
                            tvFans.setText(profile.optInt("followeds") + " 粉丝");

                            MusicDbHelper db = new MusicDbHelper(requireContext());
                            android.database.Cursor c = db.getUserProfile(currentUid);

                            String localNick = null;
                            String localAvatar = null;
                            if (c != null && c.moveToFirst()) {
                                localNick = c.getString(c.getColumnIndexOrThrow("nickname"));
                                localAvatar = c.getString(c.getColumnIndexOrThrow("avatarPath"));
                            }
                            if (c != null) c.close();
                            db.close();

                            // 独立判断：如果本地没有昵称（为空），就使用网络获取的真实昵称兜底
                            if (localNick == null || localNick.isEmpty()) {
                                tvNickname.setText(profile.optString("nickname"));
                            }

                            // 独立判断：如果本地没有存头像，就使用网络获取的真实头像兜底
                            if (localAvatar == null || localAvatar.isEmpty()) {
                                String avatarUrl = profile.optString("avatarUrl");
                                if (!avatarUrl.isEmpty()) {
                                    Glide.with(requireContext()).load(avatarUrl).placeholder(R.drawable.ic_logo).circleCrop().into(ivAvatar);
                                }
                            }
                        });

                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }

    private void showCreatePlaylistDialog() {
        EditText et = new EditText(getContext());
        new AlertDialog.Builder(getContext())
                .setTitle("新建本地歌单")
                .setView(et)
                .setPositiveButton("创建", (d, w) -> {
                    String name = et.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(getContext(), "名称不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String localPid = "LOCAL_" + System.currentTimeMillis();
                    MusicDbHelper dbHelper = new MusicDbHelper(requireContext());
                    dbHelper.createLocalPlaylist(localPid, name, "");
                    dbHelper.close();

                    Toast.makeText(getContext(), "本地歌单创建成功！", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(getActivity(), MyPlaylistDetailActivity.class);
                    intent.putExtra("playlistId", localPid);
                    intent.putExtra("playlistName", name);
                    intent.putExtra("playlistCover", "");
                    startActivity(intent);

                }).setNegativeButton("取消", null).show();
    }

    private void fetchUserEvent() {
        apiService.getUserEvent(currentUid, 30, -1).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    layoutNotes.removeAllViews();
                    JSONObject json = new JSONObject(response.body().string());
                    JSONArray events = json.optJSONArray("events");

                    if (events == null || events.length() == 0) {
                        TextView tv = new TextView(getContext());
                        tv.setText("暂无动态笔记"); tv.setTextColor(Color.WHITE); tv.setPadding(0, 40, 0, 0);
                        layoutNotes.addView(tv);
                        return;
                    }
                    for (int i = 0; i < events.length(); i++) {
                        JSONObject event = events.getJSONObject(i);
                        JSONObject msgJson = new JSONObject(event.getString("json"));
                        TextView tv = new TextView(getContext());
                        tv.setText("📝 " + msgJson.optString("msg", "分享了内容"));
                        tv.setTextColor(Color.WHITE); tv.setPadding(0, 30, 0, 30);
                        layoutNotes.addView(tv);
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }
}