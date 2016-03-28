package com.brentvatne.react;

/**
 * Events from actual video view (e.g., ExoPlayerView or MediaPlayerView implements)
 */
public interface PlayerEventListener {
    void onLoad(String uriString, String type, boolean isNetwork);
    void onLoadComplete(long currentPosition, long mVideoDuration);
    void onPlay();
    void onPause();
    void onStop();
    void onProgress(long curPos);
    void onBuffer(int percent, long duration);
    void onSeek(long msec, long currentPosition);
    void onError(Exception e, boolean isStopOnError);

    void onVolume(float leftVolume, float rightVolume);
    void onMute(boolean muted);

}
