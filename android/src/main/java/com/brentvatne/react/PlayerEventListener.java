package com.brentvatne.react;

public interface PlayerEventListener {
    void onLoad(String uriString, String type, boolean isNetwork);
    void onLoadComplete(int currentPosition, int mVideoDuration);
    void onPlay();
    void onPause();
    void onStop();
    void onProgress(int curPos, int bufferedDuration);
    void onBuffer(int percent);
    void onSeek(int msec, int currentPosition);
    void onError(int what, int extra);

}
