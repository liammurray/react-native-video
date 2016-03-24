package com.brentvatne.react;

public interface PlayerEventListener {
    void onLoad(String uriString, String type, boolean isNetwork);
    void onLoadComplete(int currentPosition, int mVideoDuration);
    void onPlay();
    void onPause();
    void onStop();
    void onProgress(int curPos);
    void onBuffer(int percent, int duration);
    void onSeek(int msec, int currentPosition);
    void onError(int what, int extra);
    void onVolume(float leftVolume, float rightVolume);
    void onMute(boolean muted);

}
