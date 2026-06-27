package com.example.musicplayer;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

import android.widget.ImageView;

public class DiscoverFragment extends Fragment {

    private RecyclerView rvDiscoverStyles;
    private DiscoverApiService apiService;
    // 增加这两个变量
    private ImageView ivMenu;
    private TextView tvSearch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_discover, container, false);

        rvDiscoverStyles = view.findViewById(R.id.rv_discover_styles);
        rvDiscoverStyles.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // 绑定控件
        ivMenu = view.findViewById(R.id.iv_menu);
        tvSearch = view.findViewById(R.id.tv_search);

        // 设置点击事件，跳转到对应的 Activity
        if (ivMenu != null) {
            ivMenu.setOnClickListener(v -> startActivity(new Intent(getActivity(), SettingsActivity.class)));
        }
        if (tvSearch != null) {
            tvSearch.setOnClickListener(v -> startActivity(new Intent(getActivity(), SearchActivity.class)));
        }

        initRetrofit();
        loadHotPlaylistTags();

        return view;
    }

    private void initRetrofit() {
        apiService = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:3000/") // TODO: 记得换成你的IP
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(DiscoverApiService.class);
    }

    private void loadHotPlaylistTags() {
        apiService.getHotPlaylistTags().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        JSONArray tags = jsonObject.getJSONArray("tags");
                        List<StyleItem> items = new ArrayList<>();

                        for (int i = 0; i < tags.length(); i++) {
                            JSONObject tag = tags.getJSONObject(i);
                            items.add(new StyleItem(
                                    tag.getInt("id"),
                                    tag.getString("name"),
                                    "点击探索更多" + tag.getString("name")
                            ));
                        }
                        rvDiscoverStyles.setAdapter(new StyleAdapter(items));
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }

    private static class StyleItem {
        int tagId; String name; String desc;
        public StyleItem(int tagId, String name, String desc) {
            this.tagId = tagId; this.name = name; this.desc = desc;
        }
    }

    private class StyleAdapter extends RecyclerView.Adapter<StyleAdapter.StyleViewHolder> {
        private List<StyleItem> data;
        private String[] bgColors = {"#8B0000", "#00008B", "#006400", "#4B0082", "#8B4500", "#2F4F4F"};

        public StyleAdapter(List<StyleItem> data) { this.data = data; }

        @NonNull @Override
        public StyleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_discover_style, parent, false);
            return new StyleViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull StyleViewHolder holder, int position) {
            StyleItem item = data.get(position);
            holder.tvName.setText(item.name);
            holder.tvDesc.setText(item.desc);
            holder.cardView.setCardBackgroundColor(Color.parseColor(bgColors[position % bgColors.length]));


            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), StylePlaylistActivity.class);
                intent.putExtra("tagId", item.tagId);
                intent.putExtra("tagName", item.name);
                startActivity(intent);
            });
        }

        @Override public int getItemCount() { return data.size(); }

        class StyleViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvDesc;
            CardView cardView;
            public StyleViewHolder(View v) {
                super(v);
                tvName = v.findViewById(R.id.tv_style_name);
                tvDesc = v.findViewById(R.id.tv_style_desc);
                cardView = (CardView) v;
            }
        }
    }
}