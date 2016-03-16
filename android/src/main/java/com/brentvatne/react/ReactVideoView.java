package com.brentvatne.react;

import android.annotation.TargetApi;
import android.media.MediaPlayer;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.widget.MediaController;
import android.os.Handler;
import android.util.Log;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.yqritc.scalablevideoview.ScalableType;
import com.yqritc.scalablevideoview.ScalableVideoView;

public class ReactVideoView extends ScalableVideoView implements MediaPlayer.OnPreparedListener, MediaPlayer
        .OnErrorListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener, MediaController.MediaPlayerControl, ReactVideoMediaController.VisibilityListener, View.OnSystemUiVisibilityChangeListener {


    public enum Events {
        EVENT_LOAD_START("onVideoLoadStart"),
        EVENT_LOAD("onVideoLoad"),
        EVENT_ERROR("onVideoError"),
        EVENT_PROGRESS("onVideoProgress"),
        EVENT_SEEK("onVideoSeek"),
        EVENT_END("onVideoEnd");

        private final String mName;

        Events(final String name) {
            mName = name;
        }

        @Override
        public String toString() {
            return mName;
        }
    }

    public static final String EVENT_PROP_FAST_FORWARD = "canPlayFastForward";
    public static final String EVENT_PROP_SLOW_FORWARD = "canPlaySlowForward";
    public static final String EVENT_PROP_SLOW_REVERSE = "canPlaySlowReverse";
    public static final String EVENT_PROP_REVERSE = "canPlayReverse";
    public static final String EVENT_PROP_STEP_FORWARD = "canStepForward";
    public static final String EVENT_PROP_STEP_BACKWARD = "canStepBackward";

    public static final String EVENT_PROP_DURATION = "duration";
    public static final String EVENT_PROP_PLAYABLE_DURATION = "playableDuration";
    public static final String EVENT_PROP_CURRENT_TIME = "currentTime";
    public static final String EVENT_PROP_SEEK_TIME = "seekTime";

    public static final String EVENT_PROP_ERROR = "error";
    public static final String EVENT_PROP_WHAT = "what";
    public static final String EVENT_PROP_EXTRA = "extra";

    private ThemedReactContext mThemedReactContext;
    private RCTEventEmitter mEventEmitter;

    private Handler mProgressUpdateHandler = new Handler();
    private Runnable mProgressUpdateRunnable = null;

    private String mSrcUriString = null;
    private String mSrcType = "mp4";
    private boolean mSrcIsNetwork = false;
    private boolean mSrcIsAsset = false;
    private ScalableType mResizeMode = ScalableType.LEFT_TOP;
    private boolean mRepeat = false;
    private boolean mPaused = false;
    private boolean mMuted = false;
    private boolean mShowControls = true;
    private boolean mAutoHideNav = true;
    private float mVolume = 1.0f;
    private float mRate = 1.0f;

    private boolean mMediaPlayerValid = false; // True if mMediaPlayer is in prepared, started, or paused state.
    private int mVideoDuration = 0;
    private int mVideoBufferedDuration = 0;

    private ReactVideoMediaController mController;

    private int mLastSystemUiVis;

    public ReactVideoView(ThemedReactContext themedReactContext) {
        super(themedReactContext);

        mThemedReactContext = themedReactContext;
        mEventEmitter = themedReactContext.getJSModule(RCTEventEmitter.class);

        initializeMediaPlayerIfNeeded();
        setSurfaceTextureListener(this);

        mProgressUpdateRunnable = new Runnable() {
            @Override
            public void run() {

                if (mMediaPlayerValid) {
                    WritableMap event = Arguments.createMap();
                    event.putDouble(EVENT_PROP_CURRENT_TIME, mMediaPlayer.getCurrentPosition() / 1000.0);
                    event.putDouble(EVENT_PROP_PLAYABLE_DURATION, mVideoBufferedDuration / 1000.0); //TODO:mBufferUpdateRunnable
                    mEventEmitter.receiveEvent(getId(), Events.EVENT_PROGRESS.toString(), event);
                }
                mProgressUpdateHandler.postDelayed(mProgressUpdateRunnable, 250);
            }
        };
        mProgressUpdateHandler.post(mProgressUpdateRunnable);

        setOnSystemUiVisibilityChangeListener(this);
    }

    private void initializeMediaPlayerIfNeeded() {
        if (mMediaPlayer == null) {
            mMediaPlayerValid = false;
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setScreenOnWhilePlaying(true);
            mMediaPlayer.setOnVideoSizeChangedListener(this);
            mMediaPlayer.setOnErrorListener(this);
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnBufferingUpdateListener(this);
            mMediaPlayer.setOnCompletionListener(this);
        }
    }

    public void setSrc(final String uriString, final String type, final boolean isNetwork, final boolean isAsset) {
        mSrcUriString = uriString;
        mSrcType = type;
        mSrcIsNetwork = isNetwork;
        mSrcIsAsset = isAsset;

        mMediaPlayerValid = false;
        mVideoDuration = 0;
        mVideoBufferedDuration = 0;

        initializeMediaPlayerIfNeeded();
        mMediaPlayer.reset();

        try {
            if (isNetwork || isAsset) {
                setDataSource(uriString);
            } else {
                setRawData(mThemedReactContext.getResources().getIdentifier(
                        uriString,
                        "raw",
                        mThemedReactContext.getPackageName()
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        WritableMap src = Arguments.createMap();
        src.putString(ReactVideoViewManager.PROP_SRC_URI, uriString);
        src.putString(ReactVideoViewManager.PROP_SRC_TYPE, type);
        src.putBoolean(ReactVideoViewManager.PROP_SRC_IS_NETWORK, isNetwork);
        WritableMap event = Arguments.createMap();
        event.putMap(ReactVideoViewManager.PROP_SRC, src);
        mEventEmitter.receiveEvent(getId(), Events.EVENT_LOAD_START.toString(), event);

        prepareAsync(this);
    }

    public void setResizeModeModifier(final ScalableType resizeMode) {
        mResizeMode = resizeMode;

        if (mMediaPlayerValid) {
            setScalableType(resizeMode);
            invalidate();
        }
    }

    public void setRepeatModifier(final boolean repeat) {
        mRepeat = repeat;

        if (mMediaPlayerValid) {
            setLooping(repeat);
        }
    }

    public void setPausedModifier(final boolean paused) {
        mPaused = paused;

        if (!mMediaPlayerValid) {
            return;
        }

        if (mPaused) {
            if (mMediaPlayer.isPlaying()) {
                pause();
            }
        } else {
            if (!mMediaPlayer.isPlaying()) {
                start();
            }
        }
    }

    public void setMutedModifier(final boolean muted) {
        mMuted = muted;

        if (!mMediaPlayerValid) {
            return;
        }

        if (mMuted) {
            setVolume(0, 0);
        } else {
            setVolume(mVolume, mVolume);
        }
    }

    public void setVolumeModifier(final float volume) {
        mVolume = volume;
        setMutedModifier(mMuted);
    }

    public void setAutoHideNav(final boolean autoHideNav) {
        mAutoHideNav = autoHideNav;
    }
    public void setShowControls(final boolean showControls) {
        mShowControls = showControls;
        if (mShowControls) {
            if (mController == null) {
                mController = new ReactVideoMediaController(mThemedReactContext);
                mController.setMediaPlayer(this);
                mController.setAnchorView(this);
                mController.setVisibilityListener(this);
            }
            if (!mController.isShowing()) {
                mController.show();
            }
        } else if (mController != null) {
            mController.hide();
        }
    }

    public void setRateModifier(final float rate) {
        mRate = rate;

        if (mMediaPlayerValid) {
            // TODO: Implement this.
            Log.e(ReactVideoViewManager.REACT_CLASS, "Setting playback rate is not yet supported on Android");
        }
    }

    public void applyModifiers() {
        setResizeModeModifier(mResizeMode);
        setRepeatModifier(mRepeat);
        setPausedModifier(mPaused);
        setMutedModifier(mMuted);
        setShowControls(mShowControls);
        setAutoHideNav(mAutoHideNav);
//        setRateModifier(mRate);
    }

    @Override
    public void onSystemUiVisibilityChange(int visibility) {
        int diff = mLastSystemUiVis ^ visibility;
        mLastSystemUiVis = visibility;
        if (mAutoHideNav) {
            // If SYSTEM_UI_FLAG_HIDE_NAVIGATION removed from visibility flags...
            if ((diff & SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0
                    && (visibility & SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
                setNavVisibility(true);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mController != null) {
            toggleMediaControllerVisibility();
        }
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mMediaPlayerValid = true;
        mVideoDuration = mp.getDuration();

        WritableMap event = Arguments.createMap();
        event.putDouble(EVENT_PROP_DURATION, mVideoDuration / 1000.0);
        event.putDouble(EVENT_PROP_CURRENT_TIME, mp.getCurrentPosition() / 1000.0);
        // TODO: Actually check if you can.
        event.putBoolean(EVENT_PROP_FAST_FORWARD, true);
        event.putBoolean(EVENT_PROP_SLOW_FORWARD, true);
        event.putBoolean(EVENT_PROP_SLOW_REVERSE, true);
        event.putBoolean(EVENT_PROP_REVERSE, true);
        event.putBoolean(EVENT_PROP_FAST_FORWARD, true);
        event.putBoolean(EVENT_PROP_STEP_BACKWARD, true);
        event.putBoolean(EVENT_PROP_STEP_FORWARD, true);
        mEventEmitter.receiveEvent(getId(), Events.EVENT_LOAD.toString(), event);

        applyModifiers();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        WritableMap error = Arguments.createMap();
        error.putInt(EVENT_PROP_WHAT, what);
        error.putInt(EVENT_PROP_EXTRA, extra);
        WritableMap event = Arguments.createMap();
        event.putMap(EVENT_PROP_ERROR, error);
        mEventEmitter.receiveEvent(getId(), Events.EVENT_ERROR.toString(), event);
        return true;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        mVideoBufferedDuration = (int) Math.round((double) (mVideoDuration * percent) / 100.0);
    }

    @Override
    public void seekTo(int msec) {

        if (mMediaPlayerValid) {
            WritableMap event = Arguments.createMap();
            event.putDouble(EVENT_PROP_CURRENT_TIME, getCurrentPosition() / 1000.0);
            event.putDouble(EVENT_PROP_SEEK_TIME, msec / 1000.0);
            mEventEmitter.receiveEvent(getId(), Events.EVENT_SEEK.toString(), event);

            super.seekTo(msec);
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mMediaPlayerValid = false;
        mEventEmitter.receiveEvent(getId(), Events.EVENT_END.toString(), null);
        if (mController != null) {
            mController.show(0);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        mMediaPlayerValid = false;
        if (mController != null) {
            // Ensures controller view removed from window manager
            mController.hide();
            mController = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setSrc(mSrcUriString, mSrcType, mSrcIsNetwork, mSrcIsAsset);
        setShowControls(mShowControls);
    }


    @Override
    public void onControllerVisibilityChanged(boolean attached) {
        if (mAutoHideNav) {
            setNavVisibility(attached);
        }
    }

    private void setNavVisibility(boolean visible) {
        /**
         * SYSTEM_UI_FLAG_LAYOUT_XXX: size content area to include area behind system bars (bars overlap)
         * SYSTEM_UI_FLAG_LOW_PROFILE: dims status and nav bar
         * SYSTEM_UI_FLAG_FULLSCREEN: hide status bar
         * SYSTEM_UI_FLAG_HIDE_NAVIGATION: hide nav bar
         * SYSTEM_UI_FLAG_IMMERSIVE (and SYSTEM_UI_FLAG_IMMERSIVE_STICKY): don't capture touch events near bars
         */

        // These should be set by activity. If we change them later there are artifacts.
        int newVis = SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | SYSTEM_UI_FLAG_LAYOUT_STABLE;
        newVis &= getSystemUiVisibility();

        if (!visible) {
            newVis |= SYSTEM_UI_FLAG_FULLSCREEN | SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        }
        setSystemUiVisibility(newVis);
        if (mController != null) {
            // In case where we set nav visibility outside of listener that observes media controller visibility changes
            setMediaControllerVisibility(visible);
        }
    }

    private void setMediaControllerVisibility(boolean setVisible) {
        if (mController.isShowing()) {
            if (!setVisible) {
                mController.hide();
            }
        } else {
            if (setVisible) {
                mController.show(0);
            }
        }
    }

    private void toggleMediaControllerVisibility() {
        if (mController.isShowing()) {
            mController.hide();
        } else {
            mController.show();
        }
    }

    //////////////////////////////////////////////////////////////////
    // Interface MediaController.MediaPlayerControl
    //////////////////////////////////////////////////////////////////
    public  int getBufferPercentage() {
        return mVideoDuration > 0 ? mVideoBufferedDuration * 100 / mVideoDuration : mVideoDuration;
    }

    public void pause() {
        super.pause();
        if (mController != null) {
            mController.show(0);
        }
    }

    public void start() {
        super.start();
        if (mController != null) {
            mController.show();
        }
    }

    public  boolean canPause() {
        return true;
    }

    public  boolean canSeekBackward() {
        return true;
    }

    public  boolean canSeekForward() {
        return true;
    }

    public  int getAudioSessionId() {
        return mMediaPlayer.getAudioSessionId();
    }
}
