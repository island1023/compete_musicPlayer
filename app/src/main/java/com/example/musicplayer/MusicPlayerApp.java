package com.example.musicplayer;

import android.app.Application;
import com.google.android.exoplayer2.ExoPlayer;

public class MusicPlayerApp extends Application {
    private static MusicPlayerApp instance;
    private ExoPlayer exoPlayer;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        exoPlayer = new ExoPlayer.Builder(this).build();
    }

    public static MusicPlayerApp getInstance() {
        return instance;
    }

    public ExoPlayer getExoPlayer() {
        return exoPlayer;
    }
}