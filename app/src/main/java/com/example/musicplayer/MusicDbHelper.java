package com.example.musicplayer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MusicDbHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "MusicPlayer.db";
    private static final int DB_VERSION = 6;

    public MusicDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE liked_songs (songId TEXT PRIMARY KEY, songName TEXT, artistName TEXT, coverUrl TEXT)");
        db.execSQL("CREATE TABLE recent_songs (songId TEXT PRIMARY KEY, songName TEXT, artistName TEXT, coverUrl TEXT, playTime INTEGER)");
        db.execSQL("CREATE TABLE user_profile (uid TEXT PRIMARY KEY, nickname TEXT, gender INTEGER, birthday INTEGER, signature TEXT, avatarPath TEXT)");
        db.execSQL("CREATE TABLE local_playlists (pid TEXT PRIMARY KEY, name TEXT, coverPath TEXT)");
        db.execSQL("CREATE TABLE playlist_songs (pid TEXT, songId TEXT, songName TEXT, artistName TEXT, coverUrl TEXT)");
        db.execSQL("CREATE TABLE fav_playlists (pid TEXT PRIMARY KEY, name TEXT, coverUrl TEXT)");

        // 🌟 新增表：收藏的歌手、收藏的主播、本地评论
        db.execSQL("CREATE TABLE fav_artists (artistId TEXT PRIMARY KEY, artistName TEXT, artistPic TEXT)");
        db.execSQL("CREATE TABLE fav_anchors (anchorId TEXT PRIMARY KEY, anchorName TEXT, anchorPic TEXT)");
        db.execSQL("CREATE TABLE local_comments (id INTEGER PRIMARY KEY AUTOINCREMENT, songId TEXT, content TEXT, timestamp INTEGER)");
        db.execSQL("CREATE TABLE mv_history (videoId TEXT PRIMARY KEY, title TEXT, creator TEXT, coverUrl TEXT, playTime INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS liked_songs");
        db.execSQL("DROP TABLE IF EXISTS recent_songs");
        db.execSQL("DROP TABLE IF EXISTS user_profile");
        db.execSQL("DROP TABLE IF EXISTS local_playlists");
        db.execSQL("DROP TABLE IF EXISTS playlist_songs");
        db.execSQL("DROP TABLE IF EXISTS fav_playlists");

        // 🌟 新增表的销毁重建
        db.execSQL("DROP TABLE IF EXISTS fav_artists");
        db.execSQL("DROP TABLE IF EXISTS fav_anchors");
        db.execSQL("DROP TABLE IF EXISTS local_comments");
        db.execSQL("DROP TABLE IF EXISTS mv_history");
        onCreate(db);
    }

    // ================== 🌟 新增：收藏歌手 / 主播 ==================
    public void toggleFavArtist(String id, String name, String pic, boolean isFav) {
        SQLiteDatabase db = getWritableDatabase();
        if (isFav) {
            ContentValues values = new ContentValues();
            values.put("artistId", id);
            values.put("artistName", name);
            values.put("artistPic", pic);
            db.replace("fav_artists", null, values);
        } else {
            db.delete("fav_artists", "artistId=?", new String[]{id});
        }
    }

    public boolean isFavArtist(String id) {
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM fav_artists WHERE artistId=?", new String[]{id});
        boolean exists = c.getCount() > 0;
        c.close();
        return exists;
    }

    // 💡 补充查询方法：供后续“我的-收藏的歌手”列表页使用
    public List<Map<String, String>> getFavArtists() {
        List<Map<String, String>> list = new ArrayList<>();
        Cursor cursor = getReadableDatabase().rawQuery("SELECT * FROM fav_artists", null);
        while (cursor.moveToNext()) {
            Map<String, String> map = new HashMap<>();
            map.put("id", cursor.getString(cursor.getColumnIndexOrThrow("artistId")));
            map.put("name", cursor.getString(cursor.getColumnIndexOrThrow("artistName")));
            map.put("picUrl", cursor.getString(cursor.getColumnIndexOrThrow("artistPic")));
            list.add(map);
        }
        cursor.close();
        return list;
    }

    public void toggleFavAnchor(String id, String name, String pic, boolean isFav) {
        SQLiteDatabase db = getWritableDatabase();
        if (isFav) {
            ContentValues values = new ContentValues();
            values.put("anchorId", id);
            values.put("anchorName", name);
            values.put("anchorPic", pic);
            db.replace("fav_anchors", null, values);
        } else {
            db.delete("fav_anchors", "anchorId=?", new String[]{id});
        }
    }

    public boolean isFavAnchor(String id) {
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM fav_anchors WHERE anchorId=?", new String[]{id});
        boolean exists = c.getCount() > 0;
        c.close();
        return exists;
    }

    // 💡 补充查询方法：供后续“我的-收藏的主播”列表页使用
    public List<Map<String, String>> getFavAnchors() {
        List<Map<String, String>> list = new ArrayList<>();
        Cursor cursor = getReadableDatabase().rawQuery("SELECT * FROM fav_anchors", null);
        while (cursor.moveToNext()) {
            Map<String, String> map = new HashMap<>();
            map.put("id", cursor.getString(cursor.getColumnIndexOrThrow("anchorId")));
            map.put("name", cursor.getString(cursor.getColumnIndexOrThrow("anchorName")));
            map.put("picUrl", cursor.getString(cursor.getColumnIndexOrThrow("anchorPic")));
            list.add(map);
        }
        cursor.close();
        return list;
    }

    // ================== 🌟 新增：本地评论功能 ==================
    public void addLocalComment(String songId, String content) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("songId", songId);
        values.put("content", content);
        values.put("timestamp", System.currentTimeMillis());
        db.insert("local_comments", null, values);
    }

    // 🌟 核心修改：改为返回 LocalCommentModel 列表，携带数据库的主键 ID
    public List<LocalCommentModel> getLocalComments(String songId) {
        List<LocalCommentModel> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT id, content FROM local_comments WHERE songId=? ORDER BY timestamp DESC", new String[]{songId});
        while (c.moveToNext()) {
            list.add(new LocalCommentModel(c.getInt(0), c.getString(1)));
        }
        c.close();
        return list;
    }

    // ================== ⬇️ 以下为原封不动保留的所有旧方法 ==================

    /*// --- 用户资料本地化 ---
    public void saveUserProfile(String uid, String nickname, int gender, long birthday, String signature, String avatarPath) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("uid", uid);
        if (nickname != null) values.put("nickname", nickname);
        values.put("gender", gender);
        values.put("birthday", birthday);
        if (signature != null) values.put("signature", signature);
        if (avatarPath != null) values.put("avatarPath", avatarPath);
        db.replace("user_profile", null, values);
    }
*/
    // --- 用户资料本地化 ---
    public void saveUserProfile(String uid, String nickname, int gender, long birthday, String signature, String avatarPath) {
        SQLiteDatabase db = getWritableDatabase();

        // 1. 先查询本地是否已经有该用户的数据
        Cursor cursor = db.rawQuery("SELECT * FROM user_profile WHERE uid=?", new String[]{uid});
        if (cursor != null && cursor.moveToFirst()) {
            // 2. 如果之前有数据，当传入 null 时，使用旧数据填补，避免被覆盖为空！
            if (nickname == null) nickname = cursor.getString(cursor.getColumnIndexOrThrow("nickname"));
            if (gender == -1) gender = cursor.getInt(cursor.getColumnIndexOrThrow("gender"));
            if (birthday == -1) birthday = cursor.getLong(cursor.getColumnIndexOrThrow("birthday"));
            if (signature == null) signature = cursor.getString(cursor.getColumnIndexOrThrow("signature"));
            if (avatarPath == null) avatarPath = cursor.getString(cursor.getColumnIndexOrThrow("avatarPath"));
        }
        if (cursor != null) {
            cursor.close();
        }

        // 3. 将合并后的完整数据重新存入
        ContentValues values = new ContentValues();
        values.put("uid", uid);
        values.put("nickname", nickname);
        values.put("gender", gender);
        values.put("birthday", birthday);
        values.put("signature", signature);
        values.put("avatarPath", avatarPath);

        db.replace("user_profile", null, values);
    }

    public Cursor getUserProfile(String uid) {
        return getReadableDatabase().rawQuery("SELECT * FROM user_profile WHERE uid=?", new String[]{uid});
    }

    // --- 本地歌单功能 ---
    public void createLocalPlaylist(String pid, String name, String coverPath) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("pid", pid);
        values.put("name", name);
        values.put("coverPath", coverPath);
        db.insert("local_playlists", null, values);
    }

    public List<Map<String, String>> getLocalPlaylists() {
        List<Map<String, String>> list = new ArrayList<>();
        Cursor cursor = getReadableDatabase().rawQuery("SELECT * FROM local_playlists", null);
        while (cursor.moveToNext()) {
            Map<String, String> map = new HashMap<>();
            // 读取数据库真实的列名，并映射为适配器能直接读取的键名
            map.put("pid", cursor.getString(cursor.getColumnIndexOrThrow("pid")));
            map.put("name", cursor.getString(cursor.getColumnIndexOrThrow("name")));
            map.put("coverUrl", cursor.getString(cursor.getColumnIndexOrThrow("coverPath")));
            list.add(map);
        }
        cursor.close();
        return list;
    }

    public void addSongToPlaylist(String pid, String songId, String songName, String artistName, String coverUrl) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("pid", pid);
        values.put("songId", songId);
        values.put("songName", songName);
        values.put("artistName", artistName);
        values.put("coverUrl", coverUrl);
        db.insert("playlist_songs", null, values);
    }

    // --- 收藏歌单功能 ---
    public void toggleFavPlaylist(String pid, String name, String coverUrl, boolean isFav) {
        SQLiteDatabase db = getWritableDatabase();
        if (isFav) {
            ContentValues values = new ContentValues();
            values.put("pid", pid);
            values.put("name", name);
            values.put("coverUrl", coverUrl);
            db.replace("fav_playlists", null, values);
        } else {
            db.delete("fav_playlists", "pid=?", new String[]{pid});
        }
    }

    // --- 喜欢的音乐 ---
    public boolean isLiked(String songId) {
        Cursor cursor = getReadableDatabase().rawQuery("SELECT * FROM liked_songs WHERE songId=?", new String[]{songId});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public void toggleLike(String songId, String songName, String artistName, String coverUrl, boolean isLike) {
        SQLiteDatabase db = getWritableDatabase();
        if (isLike) {
            ContentValues values = new ContentValues();
            values.put("songId", songId);
            values.put("songName", songName);
            values.put("artistName", artistName);
            values.put("coverUrl", coverUrl);
            db.replace("liked_songs", null, values);
        } else {
            db.delete("liked_songs", "songId=?", new String[]{songId});
        }
    }

    public List<Map<String, String>> getLikedSongs() {
        return querySongs("SELECT * FROM liked_songs");
    }

    // --- 最近播放 ---
    public void addRecentSong(String songId, String songName, String artistName, String coverUrl) {
        SQLiteDatabase db = getWritableDatabase();
        // 第一步：先尝试删除这首歌（如果它以前播过，把它删掉）
        db.delete("recent_songs", "songId=?", new String[]{songId});
        // 第二步：使用最新的时间戳重新插入这首歌
        ContentValues values = new ContentValues();
        values.put("songId", songId);
        values.put("songName", songName);
        values.put("artistName", artistName);
        values.put("coverUrl", coverUrl);
        values.put("playTime", System.currentTimeMillis());
        db.insert("recent_songs", null, values);
    }

    public List<Map<String, String>> getRecentSongs() {
        return querySongs("SELECT * FROM recent_songs ORDER BY playTime DESC LIMIT 50");
    }

    private List<Map<String, String>> querySongs(String sql) {
        List<Map<String, String>> list = new ArrayList<>();
        Cursor cursor = getReadableDatabase().rawQuery(sql, null);
        while (cursor.moveToNext()) {
            Map<String, String> map = new HashMap<>();
            map.put("songId", cursor.getString(cursor.getColumnIndexOrThrow("songId")));
            map.put("title", cursor.getString(cursor.getColumnIndexOrThrow("songName")));
            map.put("artist", cursor.getString(cursor.getColumnIndexOrThrow("artistName")));
            map.put("coverUrl", cursor.getString(cursor.getColumnIndexOrThrow("coverUrl")));
            list.add(map);
        }
        cursor.close();
        return list;
    }

    public List<Map<String, String>> getFavPlaylists() {
        List<Map<String, String>> list = new ArrayList<>();
        Cursor cursor = getReadableDatabase().rawQuery("SELECT * FROM fav_playlists", null);
        while (cursor.moveToNext()) {
            Map<String, String> map = new HashMap<>();
            map.put("pid", cursor.getString(cursor.getColumnIndexOrThrow("pid")));
            map.put("name", cursor.getString(cursor.getColumnIndexOrThrow("name")));
            map.put("coverUrl", cursor.getString(cursor.getColumnIndexOrThrow("coverUrl")));
            list.add(map);
        }
        cursor.close();
        return list;
    }

    // --- 补充：删除本地歌单 ---
    public void deleteLocalPlaylist(String pid) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("local_playlists", "pid=?", new String[]{pid});
        db.delete("playlist_songs", "pid=?", new String[]{pid});
    }

    // --- 补充：更新本地歌单封面 ---
    public void updateLocalPlaylistCover(String pid, String coverPath) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("coverPath", coverPath);
        db.update("local_playlists", values, "pid=?", new String[]{pid});
    }

    // --- 补充：读取某个本地歌单下的所有歌曲 ---
    public List<Map<String, String>> getPlaylistSongs(String pid) {
        List<Map<String, String>> list = new ArrayList<>();
        Cursor cursor = getReadableDatabase().rawQuery("SELECT * FROM playlist_songs WHERE pid=?", new String[]{pid});
        while (cursor.moveToNext()) {
            Map<String, String> map = new HashMap<>();
            map.put("songId", cursor.getString(cursor.getColumnIndexOrThrow("songId")));
            map.put("name", cursor.getString(cursor.getColumnIndexOrThrow("songName")));
            map.put("artist", cursor.getString(cursor.getColumnIndexOrThrow("artistName")));
            map.put("coverUrl", cursor.getString(cursor.getColumnIndexOrThrow("coverUrl")));
            list.add(map);
        }
        cursor.close();
        return list;
    }

    // --- 补充：查询某个歌单是否已被收藏 ---
    public boolean isFavPlaylist(String pid) {
        Cursor cursor = getReadableDatabase().rawQuery("SELECT * FROM fav_playlists WHERE pid=?", new String[]{pid});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }


    // ================== 🌟 新增：MV 播放记录功能 ==================
    public void addMvHistory(String videoId, String title, String creator, String coverUrl) {
        SQLiteDatabase db = getWritableDatabase();
        // 覆盖去重
        db.delete("mv_history", "videoId=?", new String[]{videoId});

        ContentValues values = new ContentValues();
        values.put("videoId", videoId);
        values.put("title", title);
        values.put("creator", creator);
        values.put("coverUrl", coverUrl);
        values.put("playTime", System.currentTimeMillis());
        db.insert("mv_history", null, values);
    }

    public List<Map<String, String>> getMvHistory() {
        List<Map<String, String>> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM mv_history ORDER BY playTime DESC LIMIT 50", null);
        while (c.moveToNext()) {
            Map<String, String> map = new HashMap<>();
            map.put("videoId", c.getString(c.getColumnIndexOrThrow("videoId")));
            map.put("title", c.getString(c.getColumnIndexOrThrow("title")));
            map.put("creator", c.getString(c.getColumnIndexOrThrow("creator")));
            map.put("coverUrl", c.getString(c.getColumnIndexOrThrow("coverUrl")));
            list.add(map);
        }
        c.close();
        return list;
    }


    // ================== 🌟 新增：本地评论功能 ==================
    // ================== 🌟 新增：本地评论功能 ==================
    // 增加一个内部类用来保存评论的完整信息（包含数据库自增ID）
    public static class LocalCommentModel {
        public int id;
        public String content;
        public LocalCommentModel(int id, String content) {
            this.id = id;
            this.content = content;
        }
    }


    // 🌟 新增：删除本地评论
    public void deleteLocalComment(int commentId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("local_comments", "id=?", new String[]{String.valueOf(commentId)});
    }

    // 🌟 新增：更新本地评论
    public void updateLocalComment(int commentId, String newContent) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("content", newContent);
        db.update("local_comments", values, "id=?", new String[]{String.valueOf(commentId)});
    }
}