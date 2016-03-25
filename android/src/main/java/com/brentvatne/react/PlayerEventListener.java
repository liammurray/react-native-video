package com.brentvatne.react;

public interface PlayerEventListener {
    void onLoad(String uriString, String type, boolean isNetwork);
    void onLoadComplete(long currentPosition, long mVideoDuration);
    void onPlay();
    void onPause();
    void onStop();
    void onProgress(long curPos);
    void onBuffer(int percent, long duration);
    void onSeek(long msec, long currentPosition);
    void onError(int what, int extra);
    void onVolume(float leftVolume, float rightVolume);
    void onMute(boolean muted);

}
