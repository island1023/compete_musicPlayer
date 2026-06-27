package com.example.musicplayer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private ImageView ivMenu, ivSearch;
    private TabLayout tabHomeCategory;
    private LinearLayout layoutRecommend, layoutMusic, layoutRadio;

    // ================= 音乐 Tab =================
    private ViewPager2 vpBanner;
    private RecyclerView rvMusicPlaylists, rvMusicAlbums, rvMusicArtists, rvMusicToplist;

    // ================= 推荐 Tab =================
    private RecyclerView rvRecommendPlaylists, rvRecommendSongs, rvRecommendNew;

    // ================= 电台 Tab =================
    private ViewPager2 vpRadioBanner;
    private RecyclerView rvRadioToday, rvRadioRecommend, rvRadioHot, rvRadioPay, rvRadio24h, rvRadioNewcomer;

    // 🌟 修复：声明为 HomeApiService 而不是 AuthApiService
    private HomeApiService apiService;

    private boolean isRecommendLoaded = false;
    private boolean isMusicLoaded = false;
    private boolean isRadioLoaded = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initViews(view);

        // 🌟 修复：使用新添加的泛型方法绑定 HomeApiService
        apiService = RetrofitClient.getService(getContext(), HomeApiService.class);

        setupListeners();
        fetchMusicData(); // 默认加载

        return view;
    }

    private void initViews(View view) {
        ivMenu = view.findViewById(R.id.iv_menu);
        ivSearch = view.findViewById(R.id.iv_search);
        tabHomeCategory = view.findViewById(R.id.tab_home_category);

        layoutRecommend = view.findViewById(R.id.layout_recommend);
        layoutMusic = view.findViewById(R.id.layout_music);
        layoutRadio = view.findViewById(R.id.layout_radio);

        vpBanner = view.findViewById(R.id.vp_banner);
        rvMusicPlaylists = view.findViewById(R.id.rv_music_playlists);
        rvMusicAlbums = view.findViewById(R.id.rv_music_albums);

        if (rvMusicAlbums != null) {
            rvMusicAlbums.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            // 设置一个空的 Adapter 防止 "No adapter attached" 报错
            rvMusicAlbums.setAdapter(new HorizontalCardAdapter(getContext(), new ArrayList<>()));
        }

        rvMusicArtists = view.findViewById(R.id.rv_music_artists);
        rvMusicToplist = view.findViewById(R.id.rv_music_toplist);

        rvRecommendPlaylists = view.findViewById(R.id.rv_recommend_playlists);
        rvRecommendSongs = view.findViewById(R.id.rv_recommend_songs);
        rvRecommendNew = view.findViewById(R.id.rv_recommend_new);

        vpRadioBanner = view.findViewById(R.id.vp_radio_banner);
        rvRadioToday = view.findViewById(R.id.rv_radio_today);
        rvRadioRecommend = view.findViewById(R.id.rv_radio_recommend);
        rvRadioHot = view.findViewById(R.id.rv_radio_hot);
        rvRadioPay = view.findViewById(R.id.rv_radio_pay);
        rvRadio24h = view.findViewById(R.id.rv_radio_24h);
        rvRadioNewcomer = view.findViewById(R.id.rv_radio_newcomer);

        setupHorizontal(rvMusicPlaylists); setupHorizontal(rvMusicAlbums);
        setupHorizontal(rvMusicArtists); setupHorizontal(rvMusicToplist);
        setupHorizontal(rvRecommendPlaylists); setupHorizontal(rvRecommendSongs);
        setupHorizontal(rvRecommendNew);
        setupHorizontal(rvRadioToday); setupHorizontal(rvRadioRecommend);
        setupHorizontal(rvRadioHot); setupHorizontal(rvRadioPay);
        setupHorizontal(rvRadio24h); setupHorizontal(rvRadioNewcomer);

        tabHomeCategory.getTabAt(1).select();
    }

    private void setupHorizontal(RecyclerView rv) {
        if (rv != null) rv.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
    }

    private void setupListeners() {
        tabHomeCategory.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                layoutRecommend.setVisibility(View.GONE);
                layoutMusic.setVisibility(View.GONE);
                layoutRadio.setVisibility(View.GONE);

                switch (tab.getPosition()) {
                    case 0:
                        layoutRecommend.setVisibility(View.VISIBLE);
                        if (!isRecommendLoaded) fetchRecommendData();
                        break;
                    case 1:
                        layoutMusic.setVisibility(View.VISIBLE);
                        if (!isMusicLoaded) fetchMusicData();
                        break;
                    case 2:
                        layoutRadio.setVisibility(View.VISIBLE);
                        if (!isRadioLoaded) fetchRadioData();
                        break;
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        ivMenu.setOnClickListener(v -> startActivity(new Intent(getActivity(), SettingsActivity.class)));
        ivSearch.setOnClickListener(v -> startActivity(new Intent(getActivity(), SearchActivity.class)));
    }

    private void fetchMusicData() {
        isMusicLoaded = true;

        if (vpBanner != null) {
            apiService.getBanner(1).enqueue(new Callback<ResponseBody>() {
                @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    try {
                        JSONArray banners = new JSONObject(response.body().string()).getJSONArray("banners");
                        List<String> bannerUrls = new ArrayList<>();
                        for (int i = 0; i < banners.length(); i++) bannerUrls.add(banners.getJSONObject(i).getString("pic"));
                        vpBanner.setAdapter(new BannerAdapter(bannerUrls));
                    } catch (Exception e) { e.printStackTrace(); }
                }
                @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
            });
        }

        if (rvMusicPlaylists != null) {
            apiService.getPersonalized(20).enqueue(new Callback<ResponseBody>() {
                @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    try {
                        JSONArray result = new JSONObject(response.body().string()).getJSONArray("result");
                        List<HorizontalCardAdapter.CardItem> items = new ArrayList<>();
                        for (int i = 0; i < result.length(); i++) {
                            JSONObject obj = result.getJSONObject(i);
                            items.add(new HorizontalCardAdapter.CardItem(obj.getString("id"), obj.getString("picUrl"), obj.getString("name"), HorizontalCardAdapter.TYPE_PLAYLIST));
                        }
                        rvMusicPlaylists.setAdapter(new HorizontalCardAdapter(getContext(), items));
                    } catch (Exception e) { e.printStackTrace(); }
                }
                @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
            });
        }

        // 1.3 修复版：获取新碟上架 (改用结构更稳定的 /album/new 接口)
        // 找到这部分代码并进行替换
        if (rvMusicAlbums == null) return;
        apiService.getHighqualityPlaylists(20).enqueue(new Callback<ResponseBody>() {
            @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    JSONArray playlists = new JSONObject(response.body().string()).getJSONArray("playlists");
                    List<HorizontalCardAdapter.CardItem> items = new ArrayList<>();
                    for (int i = 0; i < playlists.length(); i++) {
                        JSONObject obj = playlists.getJSONObject(i);
                        items.add(new HorizontalCardAdapter.CardItem(obj.getString("id"), obj.getString("coverImgUrl"), obj.getString("name"), HorizontalCardAdapter.TYPE_PLAYLIST));
                    }
                    if (getActivity() != null) getActivity().runOnUiThread(() -> rvMusicAlbums.setAdapter(new HorizontalCardAdapter(getContext(), items)));
                } catch (Exception e) { e.printStackTrace(); }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });

        if (rvMusicArtists != null) {
            apiService.getTopArtists(20, 0).enqueue(new Callback<ResponseBody>() {
                @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    try {
                        JSONArray artists = new JSONObject(response.body().string()).getJSONArray("artists");
                        List<HorizontalCardAdapter.CardItem> items = new ArrayList<>();
                        for (int i = 0; i < artists.length(); i++) {
                            JSONObject obj = artists.getJSONObject(i);
                            items.add(new HorizontalCardAdapter.CardItem(obj.getString("id"), obj.getString("picUrl"), obj.getString("name"), HorizontalCardAdapter.TYPE_ARTIST));
                        }
                        rvMusicArtists.setAdapter(new HorizontalCardAdapter(getContext(), items));
                    } catch (Exception e) { e.printStackTrace(); }
                }
                @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
            });
        }

        // 🌟 补充之前空缺的获取排行榜逻辑
        if (rvMusicToplist != null) {
            apiService.getToplist().enqueue(new Callback<ResponseBody>() {
                @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    try {
                        JSONArray list = new JSONObject(response.body().string()).getJSONArray("list");
                        List<HorizontalCardAdapter.CardItem> items = new ArrayList<>();
                        for (int i = 0; i < list.length(); i++) {
                            JSONObject obj = list.getJSONObject(i);
                            items.add(new HorizontalCardAdapter.CardItem(obj.getString("id"), obj.getString("coverImgUrl"), obj.getString("name"), HorizontalCardAdapter.TYPE_TOPLIST));
                        }
                        rvMusicToplist.setAdapter(new HorizontalCardAdapter(getContext(), items));
                    } catch (Exception e) { e.printStackTrace(); }
                }
                @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
            });
        }
    }

    private void fetchRecommendData() {
        isRecommendLoaded = true;

        if (rvRecommendPlaylists != null) {
            apiService.getRecommendResource().enqueue(new Callback<ResponseBody>() {
                @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    try {
                        JSONArray recommend = new JSONObject(response.body().string()).getJSONArray("recommend");
                        List<HorizontalCardAdapter.CardItem> items = new ArrayList<>();
                        for (int i = 0; i < recommend.length(); i++) {
                            JSONObject obj = recommend.getJSONObject(i);
                            items.add(new HorizontalCardAdapter.CardItem(obj.getString("id"), obj.getString("picUrl"), obj.getString("name"), HorizontalCardAdapter.TYPE_PLAYLIST));
                        }
                        rvRecommendPlaylists.setAdapter(new HorizontalCardAdapter(getContext(), items));
                    } catch (Exception e) { Toast.makeText(getContext(), "获取每日推荐需先登录", Toast.LENGTH_SHORT).show(); }
                }
                @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
            });
        }

        if (rvRecommendSongs != null) {
            apiService.getRecommendSongs(true).enqueue(new Callback<ResponseBody>() {
                @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    try {
                        JSONArray songs = new JSONObject(response.body().string()).getJSONObject("data").getJSONArray("dailySongs");
                        List<HorizontalCardAdapter.CardItem> items = new ArrayList<>();
                        for (int i = 0; i < songs.length(); i++) {
                            JSONObject obj = songs.getJSONObject(i);
                            items.add(new HorizontalCardAdapter.CardItem(obj.getString("id"), obj.getJSONObject("al").getString("picUrl"), obj.getString("name"), HorizontalCardAdapter.TYPE_SONG));
                        }
                        rvRecommendSongs.setAdapter(new HorizontalCardAdapter(getContext(), items));
                    } catch (Exception e) { e.printStackTrace(); }
                }
                @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
            });
        }

        if (rvRecommendNew != null) {
            apiService.getPersonalizedNewsong(20).enqueue(new Callback<ResponseBody>() {
                @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    try {
                        JSONArray result = new JSONObject(response.body().string()).getJSONArray("result");
                        List<HorizontalCardAdapter.CardItem> items = new ArrayList<>();
                        for (int i = 0; i < result.length(); i++) {
                            JSONObject obj = result.getJSONObject(i);
                            items.add(new HorizontalCardAdapter.CardItem(obj.getString("id"), obj.getString("picUrl"), obj.getString("name"), HorizontalCardAdapter.TYPE_SONG));
                        }
                        rvRecommendNew.setAdapter(new HorizontalCardAdapter(getContext(), items));
                    } catch (Exception e) { e.printStackTrace(); }
                }
                @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
            });
        }
    }

    private void fetchRadioData() {
        isRadioLoaded = true;

        if (vpRadioBanner != null) {
            apiService.getDjBanner().enqueue(new Callback<ResponseBody>() {
                @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    try {
                        JSONArray data = new JSONObject(response.body().string()).getJSONArray("data");
                        List<String> urls = new ArrayList<>();
                        for (int i = 0; i < data.length(); i++) urls.add(data.getJSONObject(i).getString("pic"));
                        vpRadioBanner.setAdapter(new BannerAdapter(urls));
                    } catch (Exception e) { e.printStackTrace(); }
                }
                @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
            });
        }


        // 3.2 替换为极其稳定的接口：电台精选推荐 (/dj/recommend)
        if (rvRadioToday != null) {
            // 🌟 换用 getDjRecommend()，放弃不稳定的 getDjTodayPerfered()
            apiService.getDjRecommend().enqueue(new Callback<ResponseBody>() {
                @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        // 这个接口的返回数组固定叫 djRadios，结构非常稳定
                        JSONArray djRadios = jsonObject.optJSONArray("djRadios");

                        if (djRadios != null && djRadios.length() > 0) {
                            List<HorizontalCardAdapter.CardItem> items = new ArrayList<>();
                            // 取前 20 个展示
                            int count = Math.min(djRadios.length(), 20);
                            for (int i = 0; i < count; i++) {
                                JSONObject obj = djRadios.getJSONObject(i);
                                String picUrl = obj.optString("picUrl", "") + "?param=140y140";
                                items.add(new HorizontalCardAdapter.CardItem(
                                        obj.getString("id"), picUrl, obj.getString("name"), HorizontalCardAdapter.TYPE_RADIO));
                            }
                            rvRadioToday.setAdapter(new HorizontalCardAdapter(getContext(), items));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
            });
        }

        if (rvRadioRecommend != null) {
            apiService.getDjPersonalizeRecommend(20).enqueue(new Callback<ResponseBody>() {
                @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    try {
                        JSONArray data = new JSONObject(response.body().string()).getJSONArray("data");
                        List<HorizontalCardAdapter.CardItem> items = new ArrayList<>();
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject obj = data.getJSONObject(i);
                            items.add(new HorizontalCardAdapter.CardItem(obj.getString("id"), obj.getString("picUrl"), obj.getString("name"), HorizontalCardAdapter.TYPE_RADIO));
                        }
                        rvRadioRecommend.setAdapter(new HorizontalCardAdapter(getContext(), items));
                    } catch (Exception e) { e.printStackTrace(); }
                }
                @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
            });
        }

        if (rvRadioHot != null) {
            apiService.getDjHot(20, 0).enqueue(new Callback<ResponseBody>() {
                @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    try {
                        JSONArray djRadios = new JSONObject(response.body().string()).getJSONArray("djRadios");
                        List<HorizontalCardAdapter.CardItem> items = new ArrayList<>();
                        for (int i = 0; i < djRadios.length(); i++) {
                            JSONObject obj = djRadios.getJSONObject(i);
                            items.add(new HorizontalCardAdapter.CardItem(obj.getString("id"), obj.getString("picUrl"), obj.getString("name"), HorizontalCardAdapter.TYPE_RADIO));
                        }
                        rvRadioHot.setAdapter(new HorizontalCardAdapter(getContext(), items));
                    } catch (Exception e) { e.printStackTrace(); }
                }
                @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
            });
        }

        if (rvRadioPay != null) {
            apiService.getDjPayToplist(20).enqueue(new Callback<ResponseBody>() {
                @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    try {
                        JSONArray list = new JSONObject(response.body().string()).getJSONObject("data").getJSONArray("list");
                        List<HorizontalCardAdapter.CardItem> items = new ArrayList<>();
                        for (int i = 0; i < list.length(); i++) {
                            JSONObject obj = list.getJSONObject(i);
                            items.add(new HorizontalCardAdapter.CardItem(obj.getString("id"), obj.getString("picUrl"), obj.getString("name"), HorizontalCardAdapter.TYPE_RADIO));
                        }
                        rvRadioPay.setAdapter(new HorizontalCardAdapter(getContext(), items));
                    } catch (Exception e) { e.printStackTrace(); }
                }
                @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
            });
        }

        // 3.6 24小时节目榜 (修复版：确保封面、名称、电台ID三者对齐)
        if (rvRadio24h != null) {
            apiService.getDjProgramToplistHours(15).enqueue(new Callback<ResponseBody>() {
                @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    try {
                        if (response.body() == null) return;
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        JSONArray list = jsonObject.getJSONObject("data").getJSONArray("list");
                        List<HorizontalCardAdapter.CardItem> items = new ArrayList<>();

                        for (int i = 0; i < list.length(); i++) {
                            JSONObject itemObj = list.getJSONObject(i);
                            JSONObject program = itemObj.getJSONObject("program");

                            // 🌟 严格对齐：
                            // 1. name 取 program.name
                            // 2. coverUrl 取 program.coverUrl
                            // 3. id 取 program.radio.id (保证跳转详情页准确)
                            String radioId = program.getJSONObject("radio").getString("id");
                            String name = program.getString("name");
                            String coverUrl = program.getString("coverUrl") + "?param=140y140";

                            items.add(new HorizontalCardAdapter.CardItem(radioId, coverUrl, name, HorizontalCardAdapter.TYPE_RADIO));
                        }
                        rvRadio24h.setAdapter(new HorizontalCardAdapter(getContext(), items));
                    } catch (Exception e) { e.printStackTrace(); }
                }
                @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
            });
        }

        if (rvRadioNewcomer != null) {
            apiService.getDjToplistNewcomer(15).enqueue(new Callback<ResponseBody>() {
                @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    try {
                        JSONArray list = new JSONObject(response.body().string()).getJSONObject("data").getJSONArray("list");
                        List<HorizontalCardAdapter.CardItem> items = new ArrayList<>();
                        for (int i = 0; i < list.length(); i++) {
                            JSONObject obj = list.getJSONObject(i);
                            items.add(new HorizontalCardAdapter.CardItem(obj.getString("id"), obj.getString("avatarUrl"), obj.getString("nickName"), HorizontalCardAdapter.TYPE_ARTIST));
                        }
                        rvRadioNewcomer.setAdapter(new HorizontalCardAdapter(getContext(), items));
                    } catch (Exception e) { e.printStackTrace(); }
                }
                @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
            });
        }
    }

    private class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {
        private List<String> imageUrls;
        public BannerAdapter(List<String> imageUrls) { this.imageUrls = imageUrls; }
        @NonNull @Override public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(parent.getContext());
            imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return new BannerViewHolder(imageView);
        }
        @Override public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
            Glide.with(holder.imageView.getContext()).load(imageUrls.get(position)).into(holder.imageView);
        }
        @Override public int getItemCount() { return imageUrls != null ? imageUrls.size() : 0; }
        class BannerViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            public BannerViewHolder(@NonNull View itemView) { super(itemView); this.imageView = (ImageView) itemView; }
        }
    }
}