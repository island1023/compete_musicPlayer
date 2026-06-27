package com.example.musicplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlayerActivity extends AppCompatActivity {
    private static final String TAG = "MusicDownload";

    private PopupWindow playlistPopupWindow;
    private ArrayList<String> playlistNames;

    private String songId;
    private String songName;
    private String artistName;
    private String coverUrl;

    private ImageView ivBack;
    private TextView tvSongName;
    private TextView tvArtistName;
    private ImageView ivPlayPause;
    private ImageView ivLike;
    private ImageView ivComment;
    private ImageView ivDownload;
    private ImageView ivAddToPlaylist;
    private TextView tvCurrentTime;
    private TextView tvTotalTime;
    private SeekBar sbProgress;
    private ViewPager2 vpContent;
    private RelativeLayout layoutVipBlock;
    private Button btnVipConfirm;

    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    private boolean isLiked = false;
    private String currentMp3Url = "";
    private String currentUid = "";

    private boolean isLocalMusic = false;
    private ImageView ivPrev;
    private ImageView ivNext;

    private ImageView ivPlayMode;
    private ImageView ivPlaylistMenu;
    private int currentPlayMode = 0;  // 0:列表循环, 1:单曲循环, 2:随机播放

    private ArrayList<String> playlistIds;
    private int currentIndex = -1;

    private boolean isVipSongAndNoPrivilege = false;

    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private final List<LrcLine> lrcList = new ArrayList<>();
    private LyricAdapter lyricAdapter;
    private RecyclerView rvLyrics = null;

    // --- 用于取消下载的变量 (设为 static，保证即使退出播放页面也能正常取消) ---
    private static okhttp3.Call currentDownloadCall;
    private static android.app.NotificationManager notifyManager;
    private static androidx.core.app.NotificationCompat.Builder notifyBuilder;
    private static boolean isReceiverRegistered = false;
    private int currentNotifyId; // 这个不需要 static

    // 1. 定义取消下载的广播接收器
    private static final BroadcastReceiver cancelDownloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.musicplayer.ACTION_CANCEL_DOWNLOAD".equals(intent.getAction())) {
                int notifyId = intent.getIntExtra("notifyId", -1);

                // 如果当前有正在下载的任务，立刻掐断它
                if (currentDownloadCall != null && !currentDownloadCall.isCanceled()) {
                    currentDownloadCall.cancel();
                    Toast.makeText(context, "下载已强制取消", Toast.LENGTH_SHORT).show();
                }

                // 更新通知栏状态为“已取消”，并清除“取消”按钮
                if (notifyManager != null && notifyBuilder != null && notifyId != -1) {
                    notifyBuilder.setContentText("下载已取消")
                            .setProgress(0, 0, false)
                            .setOngoing(false)
                            .clearActions(); // 移除取消按钮
                    notifyManager.notify(notifyId, notifyBuilder.build());
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        currentUid = SpUtils.getUserId(this);

        initViews();
        getIntentData();
        isLocalMusic = getIntent().getBooleanExtra("isLocal", false);

        lyricAdapter = new LyricAdapter();
        setupViewPager();
        initMediaPlayer();

        if (isLocalMusic) {
            currentMp3Url = songId;
            prepareAndPlayMusic(currentMp3Url);
            Toast.makeText(this, "正在播放本地音乐", Toast.LENGTH_SHORT).show();
        } else {
            checkMusicPrivilege();
            checkMusicAvailable();
            checkIsLiked();
            fetchRealMusicUrl();
            fetchLyrics();
        }

        setupListeners();

        // 注册取消下载的广播
        registerCancelReceiver();
    }


    // 2. 注册广播（绑定到全局 ApplicationContext）
    private void registerCancelReceiver() {
        if (!isReceiverRegistered) {
            android.content.IntentFilter filter = new android.content.IntentFilter("com.example.musicplayer.ACTION_CANCEL_DOWNLOAD");
            android.content.Context appContext = getApplicationContext();

            // 🌟 直接调用 androidx.core.content.ContextCompat 的方法，传入 RECEIVER_EXPORTED
            androidx.core.content.ContextCompat.registerReceiver(
                    appContext,
                    cancelDownloadReceiver,
                    filter,
                    androidx.core.content.ContextCompat.RECEIVER_EXPORTED
            );

            isReceiverRegistered = true;
        }
    }

    private void initViews() {
        ivBack = findViewById(R.id.iv_back);
        tvSongName = findViewById(R.id.tv_song_name);
        tvArtistName = findViewById(R.id.tv_artist_name);
        ivPlayPause = findViewById(R.id.iv_play_pause);
        ivLike = findViewById(R.id.iv_like);
        ivComment = findViewById(R.id.iv_comment);
        ivDownload = findViewById(R.id.iv_download);
        ivAddToPlaylist = findViewById(R.id.iv_add_to_playlist);

        ivAddToPlaylist.setOnClickListener(v -> showAddToPlaylistDialog());
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvTotalTime = findViewById(R.id.tv_total_time);
        sbProgress = findViewById(R.id.sb_progress);
        vpContent = findViewById(R.id.vp_content);
        layoutVipBlock = findViewById(R.id.layout_vip_block);
        btnVipConfirm = findViewById(R.id.btn_vip_confirm);
        ivPrev = findViewById(R.id.iv_prev);
        ivNext = findViewById(R.id.iv_next);
        ivPlayMode = findViewById(R.id.iv_play_mode);
        ivPlaylistMenu = findViewById(R.id.iv_playlist);
    }

    private void getIntentData() {
        songId = getIntent().getStringExtra("songId");
        if (songId == null) { songId = getIntent().getStringExtra("id"); }

        songName = getIntent().getStringExtra("songName");
        if (songName == null) { songName = getIntent().getStringExtra("name"); }

        artistName = getIntent().getStringExtra("artistName");
        if (artistName == null) { artistName = getIntent().getStringExtra("artist"); }

        coverUrl = getIntent().getStringExtra("coverUrl");
        if (coverUrl == null) { coverUrl = getIntent().getStringExtra("picUrl"); }

        tvSongName.setText(songName != null ? songName : "未知歌曲");
        tvArtistName.setText(artistName != null ? artistName : "未知歌手");

        isLocalMusic = getIntent().getBooleanExtra("isLocal", false);
        playlistIds = getIntent().getStringArrayListExtra("playlistIds");
        currentIndex = getIntent().getIntExtra("currentIndex", -1);

        if (playlistIds == null || playlistIds.size() <= 1) {
            if (ivPrev != null) {
                ivPrev.setColorFilter(Color.parseColor("#888888"));
                ivPrev.setEnabled(false);
            }
            if (ivNext != null) {
                ivNext.setColorFilter(Color.parseColor("#888888"));
                ivNext.setEnabled(false);
            }
        } else {
            if (ivPrev != null) {
                ivPrev.clearColorFilter();
                ivPrev.setEnabled(true);
            }
            if (ivNext != null) {
                ivNext.clearColorFilter();
                ivNext.setEnabled(true);
            }
        }

        playlistNames = getIntent().getStringArrayListExtra("playlistNames");
    }

    private void setupViewPager() {
        vpContent.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                if (viewType == 0) {
                    View view = inflater.inflate(R.layout.layout_player_poster, parent, false);
                    return new PosterViewHolder(view);
                } else {
                    View view = inflater.inflate(R.layout.layout_player_lyric, parent, false);
                    return new LyricViewHolder(view);
                }
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                if (holder instanceof PosterViewHolder) {
                    PosterViewHolder posterHolder = (PosterViewHolder) holder;
                    if (coverUrl != null && !coverUrl.isEmpty()) {
                        String safeCoverUrl = coverUrl.replace("http://", "https://");
                        Glide.with(posterHolder.itemView.getContext())
                                .load(safeCoverUrl)
                                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                                .placeholder(R.drawable.ic_logo)
                                .error(R.drawable.ic_logo)
                                .into(posterHolder.ivPosterInner);
                    } else {
                        posterHolder.ivPosterInner.setImageResource(R.drawable.ic_logo);
                    }
                } else if (holder instanceof LyricViewHolder) {
                    LyricViewHolder lyricHolder = (LyricViewHolder) holder;
                    rvLyrics = lyricHolder.rvLyrics;
                    rvLyrics.setLayoutManager(new LinearLayoutManager(PlayerActivity.this));
                    rvLyrics.setAdapter(lyricAdapter);
                }
            }

            @Override
            public int getItemCount() { return 2; }

            @Override
            public int getItemViewType(int position) { return position; }
        });
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());
        ivPlayPause.setOnClickListener(v -> togglePlayPause());
        ivLike.setOnClickListener(v -> toggleLike());

        if(ivPrev != null) ivPrev.setOnClickListener(v -> playNeighborSong(false));
        if(ivNext != null) ivNext.setOnClickListener(v -> playNeighborSong(true));

        if (ivPlayMode != null) {
            ivPlayMode.setOnClickListener(v -> {
                currentPlayMode = (currentPlayMode + 1) % 3;
                switch (currentPlayMode) {
                    case 0: Toast.makeText(this, "列表循环", Toast.LENGTH_SHORT).show(); break;
                    case 1: Toast.makeText(this, "单曲循环", Toast.LENGTH_SHORT).show(); break;
                    case 2: Toast.makeText(this, "随机播放", Toast.LENGTH_SHORT).show(); break;
                }
            });
        }

        if (ivPlaylistMenu != null) {
            ivPlaylistMenu.setOnClickListener(v -> {
                List<String> safeIds = playlistIds != null ? playlistIds : new ArrayList<>();
                List<String> safeNames = playlistNames != null ? playlistNames : new ArrayList<>();

                if (safeIds.isEmpty() && songId != null) {
                    safeIds.add(songId);
                    safeNames.add(songName != null ? songName : "未知歌曲");
                    currentIndex = 0;
                }

                if (safeIds.isEmpty()) return;

                List<String> displayNames = new ArrayList<>();
                for (int i = 0; i < safeIds.size(); i++) {
                    String idOrPath = safeIds.get(i);
                    String name = "";
                    if (isLocalMusic) {
                        name = new java.io.File(idOrPath).getName().replace(".mp3", "");
                    } else {
                        name = (safeNames.size() > i) ? safeNames.get(i) : "加载中...";
                    }

                    if (i == currentIndex) { name = "▶ " + name; }
                    displayNames.add(name);
                }

                View popupView = LayoutInflater.from(this).inflate(R.layout.layout_current_playlist, null);
                ListView lvPopup = popupView.findViewById(R.id.lv_popup_playlist);

                android.widget.ArrayAdapter<String> popupAdapter = new android.widget.ArrayAdapter<String>(
                        this, android.R.layout.simple_list_item_1, displayNames) {
                    @NonNull
                    @Override
                    public View getView(int position, @androidx.annotation.Nullable View convertView, @NonNull ViewGroup parent) {
                        TextView tv = (TextView) super.getView(position, convertView, parent);
                        tv.setTextColor(Color.WHITE);
                        if (position == currentIndex) {
                            tv.setTextColor(Color.parseColor("#1DB954"));
                        }
                        return tv;
                    }
                };
                lvPopup.setAdapter(popupAdapter);

                lvPopup.setOnItemClickListener((parent, view, position, id) -> {
                    if (position == currentIndex) {
                        playlistPopupWindow.dismiss();
                        return;
                    }

                    if (playlistIds == null) playlistIds = new ArrayList<>(safeIds);
                    if (playlistNames == null) playlistNames = new ArrayList<>(safeNames);

                    currentIndex = position;
                    songId = playlistIds.get(currentIndex);

                    progressHandler.removeCallbacks(updateProgressTask);
                    if (isLocalMusic) {
                        String fileName = new java.io.File(songId).getName().replace(".mp3", "");
                        tvSongName.setText(fileName);
                        tvArtistName.setText("本地音乐");
                        coverUrl = "";
                        setupViewPager();
                        prepareAndPlayMusic(songId);
                    } else {
                        tvSongName.setText("加载中...");
                        checkMusicPrivilege();
                        fetchRealMusicUrl();
                        fetchLyrics();
                        checkIsLiked();
                    }

                    playlistPopupWindow.dismiss();
                });

                int widthPx = (int) (280 * getResources().getDisplayMetrics().density);
                int heightPx = (int) (300 * getResources().getDisplayMetrics().density);
                playlistPopupWindow = new PopupWindow(popupView, widthPx, heightPx, true);
                playlistPopupWindow.setOutsideTouchable(true);
                playlistPopupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
                playlistPopupWindow.showAsDropDown(ivPlaylistMenu, -widthPx + ivPlaylistMenu.getWidth(), -heightPx - ivPlaylistMenu.getHeight());
            });
        }

        ivComment.setOnClickListener(v -> {
            if (songId == null || songId.isEmpty()) return;
            Intent intent = new Intent(PlayerActivity.this, CommentActivity.class);
            intent.putExtra("songId", songId);
            intent.putExtra("type", 0);
            startActivity(intent);
        });

        ivDownload.setOnClickListener(this::downloadCurrentSong);
        btnVipConfirm.setOnClickListener(v -> layoutVipBlock.setVisibility(View.GONE));

        sbProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) { tvCurrentTime.setText(formatTime(progress)); }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer != null) { mediaPlayer.seekTo(seekBar.getProgress()); }
            }
        });
    }

    private void showAddToPlaylistDialog() {
        MusicDbHelper dbHelper = new MusicDbHelper(this);
        List<Map<String, String>> localPlaylists = dbHelper.getLocalPlaylists();
        dbHelper.close();

        if (localPlaylists == null || localPlaylists.isEmpty()) {
            Toast.makeText(this, "你还没有创建过本地歌单", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] playlistNamesArray = new String[localPlaylists.size()];
        for (int i = 0; i < localPlaylists.size(); i++) {
            playlistNamesArray[i] = localPlaylists.get(i).get("name");
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("加入歌单")
                .setItems(playlistNamesArray, (dialog, which) -> {
                    String selectedPid = localPlaylists.get(which).get("pid");
                    MusicDbHelper db = new MusicDbHelper(PlayerActivity.this);
                    db.addSongToPlaylist(selectedPid, songId, songName, artistName, coverUrl);
                    db.close();
                    Toast.makeText(PlayerActivity.this, "已成功加入歌单", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void playNeighborSong(boolean isNext) {
        if (playlistIds == null || playlistIds.isEmpty() || currentIndex == -1) return;

        if (isNext) {
            currentIndex = (currentIndex + 1) % playlistIds.size();
        } else {
            currentIndex = (currentIndex - 1 + playlistIds.size()) % playlistIds.size();
        }

        songId = playlistIds.get(currentIndex);
        progressHandler.removeCallbacks(updateProgressTask);

        if (isLocalMusic) {
            String fileName = new java.io.File(songId).getName().replace(".mp3", "");
            tvSongName.setText(fileName);
            tvArtistName.setText("本地音乐");
            coverUrl = "";
            setupViewPager();
            prepareAndPlayMusic(songId);
        } else {
            tvSongName.setText("加载中...");
            tvArtistName.setText("...");
            checkMusicPrivilege();
            fetchRealMusicUrl();
            fetchLyrics();
            checkIsLiked();
        }
    }

    private void checkMusicPrivilege() {
        if (songId == null || songId.isEmpty()) return;
        PlayerApiService apiService = RetrofitClient.getService(this, PlayerApiService.class);

        apiService.getSongDetail(songId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.body() == null) return;
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    JSONArray privileges = jsonObject.optJSONArray("privileges");
                    JSONArray songs = jsonObject.optJSONArray("songs");

                    if (songs != null && songs.length() > 0) {
                        JSONObject song = songs.getJSONObject(0);

                        String apiSongName = song.optString("name", "未知歌曲");
                        String apiArtistName = "未知歌手";
                        JSONArray arArray = song.optJSONArray("ar");
                        if (arArray != null && arArray.length() > 0) {
                            apiArtistName = arArray.getJSONObject(0).optString("name", "未知歌手");
                        }

                        songName = apiSongName;
                        artistName = apiArtistName;

                        JSONObject alObj = song.optJSONObject("al");
                        if (alObj != null) {
                            String apiCoverUrl = alObj.optString("picUrl");
                            if (apiCoverUrl != null && !apiCoverUrl.isEmpty()) {
                                coverUrl = apiCoverUrl;
                            }
                        }

                        String finalArtist = apiArtistName;
                        runOnUiThread(() -> {
                            tvSongName.setText(apiSongName);
                            tvArtistName.setText(finalArtist);
                            setupViewPager();
                        });

                        if (privileges != null && privileges.length() > 0) {
                            JSONObject priv = privileges.getJSONObject(0);
                            int fee = song.optInt("fee", 0);
                            String dlLevel = priv.optString("dlLevel", "");
                            int dl = priv.optInt("dl", -1);

                            if ((fee == 1 || fee == 4 || fee == 8) && ("none".equals(dlLevel) || dl == 0)) {
                                isVipSongAndNoPrivilege = true;
                                Log.w(TAG, "权限分析完毕: 目标歌曲是付费/VIP歌曲，且当前账号无下载权限。");
                            } else {
                                isVipSongAndNoPrivilege = false;
                            }
                        }
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }

    private void checkMusicAvailable() {
        if (songId == null || songId.isEmpty()) return;
        PlayerApiService apiService = RetrofitClient.getService(this, PlayerApiService.class);
        apiService.checkMusic(songId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.body() == null) return;
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    if (!jsonObject.optBoolean("success", true)) {
                        Toast.makeText(PlayerActivity.this, "暂无版权，可能无法正常播放", Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }

    private void checkIsLiked() {
        if (songId == null || songId.isEmpty()) return;
        MusicDbHelper dbHelper = new MusicDbHelper(this);
        isLiked = dbHelper.isLiked(songId);
        dbHelper.close();
        updateLikeUI();
    }

    private void fetchRealMusicUrl() {
        if (songId == null || songId.isEmpty()) return;
        PlayerApiService apiService = RetrofitClient.getService(this, PlayerApiService.class);
        apiService.getSongUrl(songId, "exhigh").enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.body() == null) { fetchUrlFallback(); return; }
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    JSONArray dataArray = jsonObject.optJSONArray("data");

                    if (dataArray != null && dataArray.length() > 0) {
                        JSONObject dataObj = dataArray.getJSONObject(0);
                        String realMp3Url = dataObj.optString("url", "");

                        if (!dataObj.isNull("freeTrialInfo")) {
                            isVipSongAndNoPrivilege = true;
                            Toast.makeText(PlayerActivity.this, "正在为您播放VIP试听片段", Toast.LENGTH_SHORT).show();
                        }

                        if (realMp3Url.isEmpty() || realMp3Url.equalsIgnoreCase("null")) {
                            fetchUrlFallback();
                        } else {
                            currentMp3Url = realMp3Url;
                            prepareAndPlayMusic(realMp3Url);
                        }
                    } else { fetchUrlFallback(); }
                } catch (Exception e) { e.printStackTrace(); fetchUrlFallback(); }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) { fetchUrlFallback(); }
        });
    }

    private void fetchUrlFallback() {
        PlayerApiService apiService = RetrofitClient.getService(this, PlayerApiService.class);
        apiService.getSongUrlFallback(songId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.body() == null) return;
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    JSONArray dataArray = jsonObject.optJSONArray("data");
                    if (dataArray != null && dataArray.length() > 0) {
                        JSONObject dataObj = dataArray.getJSONObject(0);
                        String fallbackUrl = dataObj.optString("url", "");

                        if (!dataObj.isNull("freeTrialInfo")) {
                            isVipSongAndNoPrivilege = true;
                        }

                        if (!fallbackUrl.isEmpty() && !fallbackUrl.equalsIgnoreCase("null")) {
                            currentMp3Url = fallbackUrl;
                            prepareAndPlayMusic(fallbackUrl);
                            return;
                        }
                    }
                    Toast.makeText(PlayerActivity.this, "无法获取可用音频播放链接", Toast.LENGTH_SHORT).show();
                } catch (Exception e) { e.printStackTrace(); }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }

    private void fetchLyrics() {
        if (songId == null || songId.isEmpty()) return;
        PlayerApiService apiService = RetrofitClient.getService(this, PlayerApiService.class);
        apiService.getLyricNew(songId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.body() == null) return;
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    if (jsonObject.has("lrc")) {
                        String fullLyric = jsonObject.getJSONObject("lrc").optString("lyric", "");
                        parseLrc(fullLyric);
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }

    private void parseLrc(String lrcString) {
        if (lrcString.isEmpty()) return;
        lrcList.clear();
        String[] lines = lrcString.split("\n");
        Pattern pattern = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)");

        for (String line : lines) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                int min = Integer.parseInt(matcher.group(1));
                int sec = Integer.parseInt(matcher.group(2));
                int ms = Integer.parseInt(matcher.group(3));
                if (matcher.group(3).length() == 2) ms *= 10;

                long totalTimeMs = min * 60000L + sec * 1000L + ms;
                String text = matcher.group(4).trim();
                lrcList.add(new LrcLine(totalTimeMs, text));
            }
        }
        if (lyricAdapter != null) lyricAdapter.notifyDataSetChanged();
    }

    private void initMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build());
        }

        mediaPlayer.setOnPreparedListener(mp -> {
            int duration = mp.getDuration();
            sbProgress.setMax(duration);
            tvTotalTime.setText(formatTime(duration));

            mp.start();
            isPlaying = true;
            ivPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            progressHandler.post(updateProgressTask);
        });

        mediaPlayer.setOnCompletionListener(mp -> {
            progressHandler.removeCallbacks(updateProgressTask);
            if (currentPlayMode == 1) {
                mediaPlayer.start();
                progressHandler.post(updateProgressTask);
            } else if (currentPlayMode == 2 && playlistIds != null && playlistIds.size() > 1) {
                currentIndex = new java.util.Random().nextInt(playlistIds.size()) - 1;
                playNeighborSong(true);
            } else if (playlistIds != null && playlistIds.size() > 1) {
                playNeighborSong(true);
            } else {
                isPlaying = false;
                ivPlayPause.setImageResource(android.R.drawable.ic_media_play);
                sbProgress.setProgress(0);
                tvCurrentTime.setText("00:00");
            }
        });

        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            Log.e(TAG, "MediaPlayer 报错: what=" + what + " extra=" + extra);
            isPlaying = false;
            ivPlayPause.setImageResource(android.R.drawable.ic_media_play);
            progressHandler.removeCallbacks(updateProgressTask);
            Toast.makeText(this, "试听结束或网络异常，即将播放下一首", Toast.LENGTH_SHORT).show();
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                playNeighborSong(true);
            }, 1500);
            return true;
        });
    }

    private void prepareAndPlayMusic(String url) {
        try {
            isPlaying = false;
            progressHandler.removeCallbacks(updateProgressTask);
            ivPlayPause.setImageResource(android.R.drawable.ic_media_play);
            tvTotalTime.setText("00:00");
            sbProgress.setProgress(0);

            mediaPlayer.reset();
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();

            MusicDbHelper dbHelper = new MusicDbHelper(this);
            dbHelper.addRecentSong(songId, songName, artistName, coverUrl);
            dbHelper.close();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "资源加载失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void togglePlayPause() {
        if (mediaPlayer == null) return;
        try {
            if (isPlaying) {
                mediaPlayer.pause();
                ivPlayPause.setImageResource(android.R.drawable.ic_media_play);
                progressHandler.removeCallbacks(updateProgressTask);
            } else {
                mediaPlayer.start();
                ivPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                progressHandler.post(updateProgressTask);
            }
            isPlaying = !isPlaying;
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void toggleLike() {
        if (songId == null || songId.isEmpty()) return;
        isLiked = !isLiked;
        MusicDbHelper dbHelper = new MusicDbHelper(this);
        dbHelper.toggleLike(songId, songName, artistName, coverUrl, isLiked);
        dbHelper.close();
        updateLikeUI();
        Toast.makeText(this, isLiked ? "已保存至本地喜欢列表" : "已从喜欢列表移除", Toast.LENGTH_SHORT).show();
    }

    private void updateLikeUI() {
        ivLike.setColorFilter(isLiked ? Color.parseColor("#FF4081") : Color.WHITE);
    }

    // 🌟 下载权限校验与启动方法
    private void downloadCurrentSong(View v) {
        if (songId == null || songId.isEmpty()) {
            Toast.makeText(this, "无效的歌曲ID", Toast.LENGTH_SHORT).show();
            return;
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1001);
                Toast.makeText(this, "请授予通知权限以查看下载进度", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Toast.makeText(this, "正在为您获取下载资源...", Toast.LENGTH_SHORT).show();

        PlayerApiService apiService = RetrofitClient.getService(this, PlayerApiService.class);
        apiService.getSongDownloadUrl(songId, "exhigh").enqueue(new retrofit2.Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(retrofit2.Call<okhttp3.ResponseBody> call, retrofit2.Response<okhttp3.ResponseBody> response) {
                try {
                    if (response.body() == null) return;
                    String json = response.body().string();
                    org.json.JSONObject jsonObject = new org.json.JSONObject(json);
                    Object dataObj = jsonObject.opt("data");
                    String downloadUrl = "";

                    if (dataObj instanceof org.json.JSONObject) {
                        downloadUrl = ((org.json.JSONObject) dataObj).optString("url", "");
                    } else if (dataObj instanceof org.json.JSONArray) {
                        org.json.JSONArray dataArr = (org.json.JSONArray) dataObj;
                        if (dataArr.length() > 0) downloadUrl = dataArr.optJSONObject(0).optString("url", "");
                    }

                    if (!downloadUrl.isEmpty() && !downloadUrl.equals("null")) {
                        final String finalUrl = downloadUrl.replace("http://", "https://");
                        runOnUiThread(() -> startCustomDownload(finalUrl));
                    } else {
                        runOnUiThread(() -> Toast.makeText(PlayerActivity.this, "暂无下载权限，请尝试其他歌曲", Toast.LENGTH_SHORT).show());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<okhttp3.ResponseBody> call, Throwable t) {
                runOnUiThread(() -> Toast.makeText(PlayerActivity.this, "网络请求失败", Toast.LENGTH_SHORT).show());
            }
        });
    }

    // 🌟 自定义的带通知栏和取消按钮的 OkHttp 下载逻辑
    private void startCustomDownload(String url) {
        String title = songName != null ? songName : "未知歌曲";
        String safeTitle = title.replaceAll("[\\\\/:*?\"<>|]", "_");

        notifyManager = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "music_download_channel";

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                    channelId, "音乐下载", android.app.NotificationManager.IMPORTANCE_LOW);
            notifyManager.createNotificationChannel(channel);
        }

        currentNotifyId = (int) System.currentTimeMillis();

        // 🌟 创建触发取消广播的 Intent
        Intent cancelIntent = new Intent("com.example.musicplayer.ACTION_CANCEL_DOWNLOAD");
        // 👇 这一行极其关键：显式指定包名，防止 Android 系统拦截该广播
        cancelIntent.setPackage(getPackageName());
        cancelIntent.putExtra("notifyId", currentNotifyId);
        int pendingFlags = android.app.PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            pendingFlags |= android.app.PendingIntent.FLAG_IMMUTABLE;
        }
        android.app.PendingIntent cancelPendingIntent = android.app.PendingIntent.getBroadcast(this, currentNotifyId, cancelIntent, pendingFlags);

        notifyBuilder = new androidx.core.app.NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("正在下载: " + safeTitle)
                .setContentText("0%")
                .setProgress(100, 0, false)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "取消", cancelPendingIntent);

        notifyManager.notify(currentNotifyId, notifyBuilder.build());

        // 🌟 核心修复：伪装成真实的电脑浏览器，防止被网易云 CDN 识别为爬虫并切断连接
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36")
                .addHeader("Referer", "https://music.163.com/")
                .build();
        currentDownloadCall = new okhttp3.OkHttpClient().newCall(request);

        currentDownloadCall.enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, java.io.IOException e) {
                if (!call.isCanceled()) {
                    notifyBuilder.setContentText("下载失败: " + e.getMessage()).setProgress(0, 0, false).setOngoing(false).clearActions();
                    notifyManager.notify(currentNotifyId, notifyBuilder.build());
                }
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws java.io.IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    notifyBuilder.setContentText("下载失败: 链接失效").setProgress(0, 0, false).setOngoing(false).clearActions();
                    notifyManager.notify(currentNotifyId, notifyBuilder.build());
                    return;
                }

                // ✅ 替换为这行新代码（使用 App 专属私有外部目录，自带 100% 读写权限）
                java.io.File dir = getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC);
                if (!dir.exists()) dir.mkdirs();

                java.io.File tempFile = new java.io.File(dir, safeTitle + ".temp");
                java.io.File finalFile = new java.io.File(dir, safeTitle + ".mp3");

                long totalBytes = response.body().contentLength();
                long downloadedBytes = 0;

                try (java.io.InputStream is = response.body().byteStream();
                     java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {

                    byte[] buffer = new byte[8192];
                    int len;
                    int lastProgress = 0;

                    while ((len = is.read(buffer)) != -1) {
                        if (call.isCanceled()) {
                            throw new java.io.IOException("Canceled by user");
                        }

                        fos.write(buffer, 0, len);
                        downloadedBytes += len;

                        if (totalBytes > 0) {
                            int progress = (int) ((downloadedBytes * 100L) / totalBytes);
                            if (progress - lastProgress >= 2 || progress == 100) {
                                lastProgress = progress;
                                notifyBuilder.setContentText("已下载 " + progress + "%").setProgress(100, progress, false);
                                notifyManager.notify(currentNotifyId, notifyBuilder.build());
                            }
                        }
                    }
                    fos.flush();
                } catch (Exception e) {
                    // 🌟 打印具体的断开原因到 Logcat
                    android.util.Log.e("MusicDownload", "下载流异常断开: " + e.getMessage(), e);

                    // 如果中途报错或被取消，立即删除没下完的临时文件
                    if (tempFile.exists()) tempFile.delete();
                    if (!call.isCanceled()) {
                        notifyBuilder.setContentText("下载被中断").setProgress(0, 0, false).setOngoing(false).clearActions();
                        notifyManager.notify(currentNotifyId, notifyBuilder.build());
                    }
                    return;
                }

                if (tempFile.exists()) {
                    tempFile.renameTo(finalFile);
                }

                notifyBuilder.setContentText("下载完成").setProgress(0, 0, false).setOngoing(false).clearActions();
                notifyManager.notify(currentNotifyId, notifyBuilder.build());

                runOnUiThread(() -> Toast.makeText(PlayerActivity.this, "下载完成: " + safeTitle, Toast.LENGTH_LONG).show());
            }
        });
    }

    private final Runnable updateProgressTask = new Runnable() {
        @Override
        public void run() {
            try {
                if (mediaPlayer != null && isPlaying) {
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    sbProgress.setProgress(currentPosition);
                    tvCurrentTime.setText(formatTime(currentPosition));

                    if (!lrcList.isEmpty() && lyricAdapter != null && rvLyrics != null) {
                        int targetIndex = -1;
                        for (int i = 0; i < lrcList.size(); i++) {
                            if (currentPosition >= lrcList.get(i).time) { targetIndex = i; } else { break; }
                        }

                        if (targetIndex != -1 && lyricAdapter.currentLineIndex != targetIndex) {
                            int previousIndex = lyricAdapter.currentLineIndex;
                            lyricAdapter.currentLineIndex = targetIndex;
                            lyricAdapter.notifyItemChanged(previousIndex);
                            lyricAdapter.notifyItemChanged(targetIndex);

                            rvLyrics.smoothScrollToPosition(targetIndex);
                        }
                    }
                    progressHandler.postDelayed(this, 350);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private String formatTime(int ms) {
        int totalSeconds = ms / 1000;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        progressHandler.removeCallbacks(updateProgressTask);
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) { mediaPlayer.stop(); }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private static class LrcLine {
        long time; String text;
        LrcLine(long time, String text) { this.time = time; this.text = text; }
    }

    private static class PosterViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPosterInner;
        PosterViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPosterInner = itemView.findViewById(R.id.iv_poster_inner);
        }
    }

    private static class LyricViewHolder extends RecyclerView.ViewHolder {
        RecyclerView rvLyrics;
        LyricViewHolder(@NonNull View itemView) {
            super(itemView);
            rvLyrics = itemView.findViewById(R.id.rv_lyrics);
        }
    }

    private class LyricAdapter extends RecyclerView.Adapter<LyricAdapter.ViewHolder> {
        int currentLineIndex = -1;

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView textView = new TextView(parent.getContext());
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            textView.setLayoutParams(lp);
            textView.setPadding(0, 28, 0, 28);
            textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            return new ViewHolder(textView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            LrcLine line = lrcList.get(position);
            holder.tvLrcRow.setText(line.text);

            if (position == currentLineIndex) {
                holder.tvLrcRow.setTextColor(Color.parseColor("#1DB954"));
                holder.tvLrcRow.setTextSize(18);
            } else {
                holder.tvLrcRow.setTextColor(Color.WHITE);
                holder.tvLrcRow.setTextSize(14);
            }
        }

        @Override public int getItemCount() { return lrcList.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvLrcRow;
            ViewHolder(View itemView) { super(itemView); tvLrcRow = (TextView) itemView; }
        }
    }
}