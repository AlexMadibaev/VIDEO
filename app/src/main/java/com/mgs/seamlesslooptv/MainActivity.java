package com.mgs.seamlesslooptv;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.media.MediaPlayer;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

public class MainActivity extends Activity implements SurfaceHolder.Callback {
    private static final int CHOOSE_VIDEO = 101;
    private static final String PREFS = "video_settings";
    private SurfaceView surface;
    private TextView hint;
    private MediaPlayer player;
    private Uri videoUri;
    private boolean surfaceReady = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(android.graphics.Color.BLACK);
        surface = new SurfaceView(this);
        surface.getHolder().addCallback(this);
        surface.setOnLongClickListener(v -> { chooseVideo(); return true; });
        root.addView(surface, new FrameLayout.LayoutParams(-1, -1));
        hint = new TextView(this);
        hint.setText("Выберите видео\n\nУдерживайте экран для замены");
        hint.setTextSize(23f);
        hint.setTextColor(android.graphics.Color.WHITE);
        hint.setGravity(Gravity.CENTER);
        hint.setOnClickListener(v -> chooseVideo());
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER);
        root.addView(hint, lp);
        setContentView(root);
        hideSystemUI();
        String stored = getSharedPreferences(PREFS, MODE_PRIVATE).getString("uri", null);
        if (stored != null) {
            videoUri = Uri.parse(stored);
            hint.setVisibility(View.GONE);
        } else {
            hint.postDelayed(this::chooseVideo, 300);
        }
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    private void chooseVideo() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("video/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, CHOOSE_VIDEO);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == CHOOSE_VIDEO && result == RESULT_OK && data != null) {
            Uri selected = data.getData();
            if (selected == null) return;
            try {
                getContentResolver().takePersistableUriPermission(selected,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException ignored) { }
            videoUri = selected;
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString("uri", selected.toString()).apply();
            hint.setVisibility(View.GONE);
            play();
        }
        hideSystemUI();
    }

    private void play() {
        if (!surfaceReady || videoUri == null) return;
        releasePlayer();
        try {
            player = new MediaPlayer();
            player.setDisplay(surface.getHolder());
            player.setScreenOnWhilePlaying(true);
            player.setDataSource(this, videoUri);
            player.setLooping(true);
            player.setOnPreparedListener(mp -> {
                hint.setVisibility(View.GONE);
                mp.start();
            });
            player.setOnVideoSizeChangedListener((mp, width, height) -> {
                if (width > 0 && height > 0) {
                    surface.getHolder().setFixedSize(width, height);
                }
            });
            player.setOnErrorListener((mp, what, extra) -> {
                hint.setText("Не удалось воспроизвести видео.\nНажмите, чтобы выбрать другое.");
                hint.setVisibility(View.VISIBLE);
                return true;
            });
            player.prepareAsync();
        } catch (Exception ex) {
            hint.setText("Ошибка файла. Нажмите для выбора другого видео.");
            hint.setVisibility(View.VISIBLE);
        }
    }

    private void releasePlayer() {
        if (player != null) {
            try { player.reset(); player.release(); }
            catch (Exception ignored) { }
            player = null;
        }
    }
    @Override public void surfaceCreated(SurfaceHolder holder) { surfaceReady = true; play(); }
    @Override public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) { }
    @Override public void surfaceDestroyed(SurfaceHolder holder) { surfaceReady = false; releasePlayer(); }
    @Override protected void onResume() { super.onResume(); hideSystemUI(); if (player != null && !player.isPlaying()) player.start(); }
    @Override protected void onDestroy() { releasePlayer(); super.onDestroy(); }
    @Override public void onBackPressed() { chooseVideo(); }
}
