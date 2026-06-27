package com.example.musicplayer;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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

public class CommentActivity extends AppCompatActivity {

    private static final String TAG = "CommentActivity";

    private String resourceId;
    private int resourceType;
    private CommentApiService apiService;

    // 视图控件
    private EditText etComment;
    private RecyclerView recyclerView;
    private View llMyCommentsSection;
    private RecyclerView rvMyComments;

    // 网络评论数据
    private CommentAdapter commentAdapter;
    private final List<CommentModel> commentList = new ArrayList<>();

    // 🌟 本地评论数据
    private MyLocalCommentAdapter myLocalCommentAdapter;
    private final List<MusicDbHelper.LocalCommentModel> myLocalComments = new ArrayList<>();
    // 用来记录当前是否正在修改某条评论，如果是，记录下它的ID
    private int editingCommentId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);

        // 1. 绑定返回按钮
        View ivBack = findViewById(R.id.iv_comment_back);
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> finish());
        }

        // 2. 接收歌曲 ID
        resourceId = getIntent().getStringExtra("songId");
        resourceType = getIntent().getIntExtra("type", 0);
        Log.d(TAG, "加载评论 - 接收到的歌曲ID = " + resourceId + " | 类型 = " + resourceType);

        // 3. 绑定 UI 控件
        etComment = findViewById(R.id.et_comment);
        recyclerView = findViewById(R.id.rv_comments);
        llMyCommentsSection = findViewById(R.id.ll_my_comments_section);
        rvMyComments = findViewById(R.id.rv_my_comments);

        // 4. 配置网络评论 RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new CommentAdapter();
        recyclerView.setAdapter(commentAdapter);

        // 🌟 5. 配置本地评论 RecyclerView
        rvMyComments.setLayoutManager(new LinearLayoutManager(this));
        myLocalCommentAdapter = new MyLocalCommentAdapter();
        rvMyComments.setAdapter(myLocalCommentAdapter);

        // 6. 初始化接口与点击事件
        apiService = RetrofitClient.getService(this, CommentApiService.class);
        findViewById(R.id.btn_send).setOnClickListener(v -> sendComment());

        // 7. 加载数据
        loadMyLocalComments(); // 先加载本地写死的评论
        loadComments();        // 再拉取网络评论
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    // 🌟 加载本地 SQLite 数据库中的我的评论
    private void loadMyLocalComments() {
        if (resourceId == null || resourceId.isEmpty()) return;

        MusicDbHelper dbHelper = new MusicDbHelper(this);
        List<MusicDbHelper.LocalCommentModel> locals = dbHelper.getLocalComments(resourceId);
        dbHelper.close();

        myLocalComments.clear();
        myLocalComments.addAll(locals);
        myLocalCommentAdapter.notifyDataSetChanged();

        llMyCommentsSection.setVisibility(myLocalComments.isEmpty() ? View.GONE : View.VISIBLE);
    }

    // 获取网络评论
    private void loadComments() {
        if (resourceId == null || resourceId.isEmpty()) {
            Toast.makeText(this, "未接收到有效的歌曲ID", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.getComments(resourceType, resourceId, 1, 1, 30, null)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        try {
                            if (response.body() == null) return;
                            String rawJson = response.body().string();
                            JSONObject jsonObject = new JSONObject(rawJson);
                            JSONObject dataObj = jsonObject.optJSONObject("data");
                            JSONArray commentsArray = (dataObj != null) ? dataObj.optJSONArray("comments") : jsonObject.optJSONArray("comments");

                            if (commentsArray != null && commentsArray.length() > 0) {
                                commentList.clear();
                                for (int i = 0; i < commentsArray.length(); i++) {
                                    JSONObject item = commentsArray.getJSONObject(i);
                                    JSONObject userObj = item.optJSONObject("user");

                                    String nickname = (userObj != null) ? userObj.optString("nickname", "热心听众") : "匿名听众";
                                    String content = item.optString("content", "");
                                    int likedCount = item.optInt("likedCount", 0);

                                    commentList.add(new CommentModel(nickname, content, likedCount));
                                }
                                runOnUiThread(() -> commentAdapter.notifyDataSetChanged());
                            } else {
                                runOnUiThread(() -> Toast.makeText(CommentActivity.this, "该歌曲暂无网络评论", Toast.LENGTH_SHORT).show());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Log.e(TAG, "评论请求失败: " + t.getMessage());
                    }
                });
    }

    // 🌟 发送评论：直接存入本地 SQLite
    private void sendComment() {
        String content = etComment.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "评论内容不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        MusicDbHelper dbHelper = new MusicDbHelper(this);
        if (editingCommentId != -1) {
            // 这是修改评论的逻辑
            dbHelper.updateLocalComment(editingCommentId, content);
            Toast.makeText(CommentActivity.this, "评论修改成功", Toast.LENGTH_SHORT).show();
            editingCommentId = -1; // 修改完毕重置状态
            // 🌟 修复: 强制转换为 Button 才能调用 setText
            ((Button) findViewById(R.id.btn_send)).setText("发送");
        } else {
            // 这是新增评论的逻辑（允许多次输入评论）
            dbHelper.addLocalComment(resourceId, content);
            Toast.makeText(CommentActivity.this, "评论发表成功", Toast.LENGTH_SHORT).show();
        }
        dbHelper.close();

        etComment.setText("");
        loadMyLocalComments();
    }

    // ==================== 内部适配器区域 ====================

    // 网络评论数据模型
    private static class CommentModel {
        String nickname;
        String content;
        int likedCount;
        CommentModel(String nickname, String content, int likedCount) {
            this.nickname = nickname;
            this.content = content;
            this.likedCount = likedCount;
        }
    }

    // 🌟 本地评论适配器
    private class MyLocalCommentAdapter extends RecyclerView.Adapter<MyLocalCommentAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // 动态构建包含文本、修改、删除的布局
            android.widget.LinearLayout container = new android.widget.LinearLayout(parent.getContext());
            container.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            container.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            container.setGravity(android.view.Gravity.CENTER_VERTICAL);
            container.setPadding(0, 16, 0, 16);

            TextView tvContent = new TextView(parent.getContext());
            tvContent.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
            tvContent.setTextColor(Color.parseColor("#448AFF"));
            tvContent.setTextSize(15);

            TextView tvEdit = new TextView(parent.getContext());
            tvEdit.setText("修改");
            tvEdit.setTextColor(Color.parseColor("#888888"));
            tvEdit.setPadding(20, 10, 20, 10);

            TextView tvDelete = new TextView(parent.getContext());
            tvDelete.setText("删除");
            tvDelete.setTextColor(Color.parseColor("#FF5252"));
            tvDelete.setPadding(20, 10, 20, 10);

            container.addView(tvContent);
            container.addView(tvEdit);
            container.addView(tvDelete);

            return new ViewHolder(container, tvContent, tvEdit, tvDelete);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MusicDbHelper.LocalCommentModel model = myLocalComments.get(position);
            holder.tvContent.setText(model.content);

            // 修改按钮事件
            holder.tvEdit.setOnClickListener(v -> {
                editingCommentId = model.id;
                etComment.setText(model.content);
                etComment.setSelection(model.content.length()); // 光标移到最后
                // 🌟 修复: 强制转换为 Button 才能调用 setText
                Button btnSend = CommentActivity.this.findViewById(R.id.btn_send);
                btnSend.setText("保存修改");
            });

            // 删除按钮事件
            holder.tvDelete.setOnClickListener(v -> {
                MusicDbHelper dbHelper = new MusicDbHelper(CommentActivity.this);
                dbHelper.deleteLocalComment(model.id);
                dbHelper.close();
                Toast.makeText(CommentActivity.this, "已删除", Toast.LENGTH_SHORT).show();
                loadMyLocalComments(); // 刷新列表

                // 如果删除了正在编辑的评论，重置输入框状态
                if (editingCommentId == model.id) {
                    editingCommentId = -1;
                    etComment.setText("");
                    ((Button) findViewById(R.id.btn_send)).setText("发送");
                }
            });
        }

        @Override
        public int getItemCount() { return myLocalComments.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvContent, tvEdit, tvDelete;
            ViewHolder(View itemView, TextView tvContent, TextView tvEdit, TextView tvDelete) {
                super(itemView);
                this.tvContent = tvContent;
                this.tvEdit = tvEdit;
                this.tvDelete = tvDelete;
            }
        }
    }

    // 🌟 这里是你之前不小心删掉的网络评论适配器，现在帮你补回来了
    private class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            android.widget.LinearLayout container = new android.widget.LinearLayout(parent.getContext());
            container.setOrientation(android.widget.LinearLayout.VERTICAL);
            container.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            container.setPadding(40, 24, 40, 24);

            TextView tvUser = new TextView(parent.getContext());
            tvUser.setTextColor(Color.parseColor("#888888"));
            tvUser.setTextSize(14);
            tvUser.setPadding(0, 0, 0, 8);

            TextView tvContent = new TextView(parent.getContext());
            tvContent.setTextColor(Color.WHITE);
            tvContent.setTextSize(15);

            container.addView(tvUser);
            container.addView(tvContent);
            return new ViewHolder(container, tvUser, tvContent);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CommentModel model = commentList.get(position);
            holder.tvUser.setText(model.nickname + " (👍 " + model.likedCount + ")");
            holder.tvContent.setText(model.content);
        }

        @Override
        public int getItemCount() { return commentList.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvUser, tvContent;
            ViewHolder(View itemView, TextView tvUser, TextView tvContent) {
                super(itemView);
                this.tvUser = tvUser;
                this.tvContent = tvContent;
            }
        }
    }
}