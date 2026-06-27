package com.example.musicplayer;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
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

public class SearchActivity extends AppCompatActivity {

    private EditText etSearch;
    private TextView tvSearchBtn;
    private TextView tvBack;
    private RecyclerView rvToplistGallery;
    private SearchApiService apiService;
    private ToplistAdapter toplistAdapter;
    private List<ToplistModel> toplistData = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        etSearch = findViewById(R.id.et_search);
        tvSearchBtn = findViewById(R.id.tv_search_btn);
        tvBack = findViewById(R.id.tv_back);
        rvToplistGallery = findViewById(R.id.rv_toplist_gallery);

        apiService = RetrofitClient.getService(this, SearchApiService.class);

        if (tvBack != null) {
            tvBack.setOnClickListener(v -> finish());
        }

        if (tvSearchBtn != null) {
            tvSearchBtn.setOnClickListener(v -> performSearch());
        }

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        rvToplistGallery.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        toplistAdapter = new ToplistAdapter(toplistData);
        rvToplistGallery.setAdapter(toplistAdapter);

        fetchDefaultSearchKeyword();
        fetchToplists();
    }

    private void performSearch() {
        String keyword = etSearch.getText().toString().trim();
        if (keyword.isEmpty() && etSearch.getHint() != null) {
            keyword = etSearch.getHint().toString();
        }

        if (!keyword.isEmpty()) {
            Intent intent = new Intent(SearchActivity.this, SearchResultActivity.class);
            intent.putExtra("keyword", keyword);
            startActivity(intent);
        } else {
            Toast.makeText(this, "请输入搜索内容", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchDefaultSearchKeyword() {
        apiService.getDefaultSearchKeyword().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.body() != null) {
                        JSONObject data = new JSONObject(response.body().string()).optJSONObject("data");
                        if (data != null) {
                            etSearch.setHint(data.optString("showKeyword"));
                        }
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }

    private void fetchToplists() {
        apiService.getToplistDetail().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.body() != null) {
                        JSONArray list = new JSONObject(response.body().string()).optJSONArray("list");
                        if (list != null) {
                            toplistData.clear();
                            for (int i = 0; i < list.length(); i++) {
                                JSONObject obj = list.getJSONObject(i);
                                JSONArray summaryTracks = obj.optJSONArray("tracks");
                                if (summaryTracks != null && summaryTracks.length() > 0) {
                                    ToplistModel model = new ToplistModel();
                                    model.id = obj.optString("id");
                                    model.name = obj.optString("name");
                                    toplistData.add(model);
                                }
                                if (toplistData.size() >= 6) break;
                            }
                            toplistAdapter.notifyDataSetChanged();
                        }
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }

    // 🌟 1. 新增：专门用于存储单首歌曲全部信息的实体类
    private class TrackModel {
        String id;
        String songName;
        String artistName;
        String coverUrl;

        TrackModel(String id, String songName, String artistName, String coverUrl) {
            this.id = id;
            this.songName = songName;
            this.artistName = artistName;
            this.coverUrl = coverUrl;
        }
    }

    // 🌟 2. 修改：榜单模型改为存储 TrackModel 对象集合
    private class ToplistModel {
        String id;
        String name;
        List<TrackModel> tracks = new ArrayList<>();
        boolean isLoading = false;
        boolean isLoaded = false;
    }

    private class ToplistAdapter extends RecyclerView.Adapter<ToplistAdapter.VH> {
        private List<ToplistModel> data;

        public ToplistAdapter(List<ToplistModel> data) { this.data = data; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int layoutId = getResources().getIdentifier("item_search_toplist", "layout", getPackageName());
            return new VH(LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {

            ToplistModel model = data.get(position);

            if (holder.name != null) {
                holder.name.setText(model.name);
            }

            if (holder.listContainer != null) {

                holder.listContainer.removeAllViews();

                if (model.isLoaded) {

                    for (int i = 0; i < model.tracks.size(); i++) {

                        TrackModel track = model.tracks.get(i);

                        TextView tv = new TextView(holder.itemView.getContext());

                        tv.setText((i + 1) + ". " + track.songName + " - " + track.artistName);
                        tv.setTextColor(Color.parseColor("#E0E0E0"));
                        tv.setTextSize(20f);
                        tv.setPadding(0, 40, 0, 40);
                        tv.setSingleLine(true);
                        tv.setEllipsize(TextUtils.TruncateAt.END);

                        tv.setOnClickListener(v -> {
                            Intent intent = new Intent(holder.itemView.getContext(), PlayerActivity.class);

                            intent.putExtra("songId", track.id);
                            intent.putExtra("songName", track.songName);
                            intent.putExtra("artistName", track.artistName);
                            intent.putExtra("coverUrl", track.coverUrl);

                            holder.itemView.getContext().startActivity(intent);
                        });

                        holder.listContainer.addView(tv);
                    }

                } else if (!model.isLoading) {

                    model.isLoading = true;

                    apiService.getTopList(model.id).enqueue(new Callback<ResponseBody>() {

                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            try {

                                if (response.body() == null) return;

                                JSONObject playlist = new JSONObject(response.body().string())
                                        .optJSONObject("playlist");

                                if (playlist == null) return;

                                JSONArray tracksArray = playlist.optJSONArray("tracks");

                                if (tracksArray == null) return;

                                int count = Math.min(tracksArray.length(), 30);

                                for (int i = 0; i < count; i++) {

                                    JSONObject trackObj = tracksArray.getJSONObject(i);

                                    String songId = trackObj.optString("id");
                                    String songName = trackObj.optString("name");

                                    JSONArray ar = trackObj.optJSONArray("ar");
                                    String artistName = (ar != null && ar.length() > 0)
                                            ? ar.getJSONObject(0).optString("name")
                                            : "未知";

                                    JSONObject al = trackObj.optJSONObject("al");
                                    String coverUrl = al != null ? al.optString("picUrl") : "";

                                    model.tracks.add(new TrackModel(songId, songName, artistName, coverUrl));
                                }

                                model.isLoaded = true;

                                // ✅ 安全获取最新 position
                                int adapterPos = holder.getBindingAdapterPosition();

                                if (adapterPos != RecyclerView.NO_POSITION) {
                                    holder.itemView.post(() ->
                                            notifyItemChanged(adapterPos)
                                    );
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {}
                    });
                }
            }
        }

        @Override public int getItemCount() { return data.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView name;
            LinearLayout listContainer;
            public VH(View v) {
                super(v);
                name = v.findViewById(R.id.tv_chart_name);
                listContainer = v.findViewById(R.id.ll_track_list);
                if (listContainer == null) {
                    listContainer = v.findViewById(getResources().getIdentifier("ll_songs_list", "id", getPackageName()));
                }
            }
        }
    }
}