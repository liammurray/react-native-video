package com.brentvatne.react;


import android.util.Log;

import com.facebook.react.bridge.ReadableMap;
import com.yqritc.scalablevideoview.ScalableType;

/**
 * Encapsulates state set from JS
 */
public class ReactVideoModelState {

    private static final String LOGTAG = ReactVideoModelState.class.getSimpleName();

    public static final String PROP_SRC = "src";
    public static final String PROP_SRC_URI = "uri";
    public static final String PROP_SRC_TYPE = "type";
    public static final String PROP_SRC_IS_NETWORK = "isNetwork";
    public static final String PROP_SRC_IS_ASSET = "isAsset";
    public static final String PROP_RESIZE_MODE = "resizeMode";
    public static final String PROP_REPEAT = "repeat";
    public static final String PROP_PAUSED = "paused";
    public static final String PROP_MUTED = "muted";
    public static final String PROP_VOLUME = "volume";
    public static final String PROP_SEEK = "seek";
    public static final String PROP_RATE = "rate";
    /** Enable MediaController */
    public static final String PROP_CONTROLS = "controls";
    /** Automatically hide navigation UI (currently only when MediaController enabled) */
    public static final String PROP_AUTOHIDE_NAV = "autoHideNav";

    private ReactVideoHostView hostView;


    private ScalableType resizeMode = ScalableType.LEFT_TOP;
    private boolean enableRepeatMode = false;
    private boolean isPaused = false;
    private boolean isMuted = false;
    private float volume = 1.0f;
    private float playbackRate = 1.0f;
    private long pos = 0;
    private boolean autoHideNav = true;

    private boolean enableControls = true;

    private String contentUri;
    private String contentType;
    private boolean contentIsNetwork;
    private boolean contentIsAsset;


    public ReactVideoModelState(ReactVideoHostView hostView) {
        this.hostView = hostView;
    }

    public void setSrc(ReadableMap src) {
        contentUri = src.hasKey(PROP_SRC_URI) ? src.getString(PROP_SRC_URI) : null;
        contentType = src.getString(PROP_SRC_TYPE);
        contentIsNetwork = src.getBoolean(PROP_SRC_IS_NETWORK);
        contentIsAsset = src.getBoolean(PROP_SRC_IS_ASSET);
        Log.d(LOGTAG, "setSrc(): " + contentUri);
        hostView.getVideoView().prepareVideo(this);
    }

    public void setResizeMode(String resizeModeOrdinalString) {
        Log.d(LOGTAG, "setResizeMode(): " + resizeModeOrdinalString);
        resizeMode = ScalableType.values()[Integer.parseInt(resizeModeOrdinalString)];
        hostView.getVideoView().setResizeMode(resizeMode);
    }


    public void setRepeat(final boolean repeat) {
        Log.d(LOGTAG, "setRepeat(): " + repeat);
        enableRepeatMode = repeat;
        hostView.getVideoView().setRepeat(repeat);
    }

    public void setPaused(final boolean paused) {
        Log.d(LOGTAG, "setPaused(): " + paused);
        isPaused = paused;
        hostView.getVideoView().setPaused(paused);
    }

    public void setMuted(final boolean muted) {
        Log.d(LOGTAG, "setMuted(): " + muted);
        isMuted = muted;
        hostView.getVideoView().setMuted(muted);
    }

    public void setVolume(final float volume) {
        //Log.d(LOGTAG, "setVolume(): " + volume);
        this.volume = volume;
        hostView.getVideoView().setVolume(volume);
    }

    public void setSeek(final float seek) {
        //Log.d(LOGTAG, "setSeek(): " + seek);
        pos = Math.round(seek * 1000.0f);
        hostView.getVideoView().seekTo(pos);
    }

    public void setRate(final float rate) {
        Log.d(LOGTAG, "setRate(): " + rate);
        playbackRate = rate;
        hostView.getVideoView().setPlaybackRate(rate);
    }

    public void setEnableControls(final boolean enableControls) {
        Log.d(LOGTAG, "setEnableControls(): " + enableControls);
        this.enableControls = enableControls;
        hostView.getContainerView().setEnableControllerView(enableControls);
    }

    public void setAutoHideNav(final boolean autoHideNav) {
        Log.d(LOGTAG, "setAutoHideNav(): " + autoHideNav);
        this.autoHideNav = autoHideNav;
        hostView.getContainerView().setAutoHideNav(autoHideNav);
    }

    public ScalableType getResizeMode() {
        return resizeMode;
    }

    public boolean isEnableRepeatMode() {
        return enableRepeatMode;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public boolean isMuted() {
        return isMuted;
    }

    public float getVolume() {
        return volume;
    }

    public float getPlaybackRate() {
        return playbackRate;
    }

    public long getPos() {
        return pos;
    }

    public boolean isAutoHideNav() {
        return autoHideNav;
    }

    public boolean isEnableControls() {
        return enableControls;
    }

    public String getContentUri() {
        return contentUri;
    }

    /** "mp4" or extension */
    public String getContentType() {
        return contentType;
    }

    public boolean isContentNetwork() {
        return contentIsNetwork;
    }

    public boolean isContentAsset() {
        return contentIsAsset;
    }

}
