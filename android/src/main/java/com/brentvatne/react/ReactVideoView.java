package com.brentvatne.react;

import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.xealth.mediacontroller.MediaControllerView;
import com.yqritc.scalablevideoview.ScalableType;
import com.yqritc.scalablevideoview.ScalableVideoView;


public class ReactVideoView extends ScalableVideoView implements MediaPlayer.OnPreparedListener, MediaPlayer
        .OnErrorListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener, MediaControllerView.VisibilityListener, View.OnSystemUiVisibilityChangeListener, MediaControllerView.MediaPlayerControl {


    public enum Events {
        EVENT_LOAD_START("onVideoLoadStart"),
        EVENT_LOAD("onVideoLoad"),
        EVENT_ENTER_FS("onVideoEnterFullScreen"),
        EVENT_EXIT_FS("onVideoExitFullScreen"),
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

    private int mLastSystemUiVis;

    private MediaControllerView mController;

    private ReactVideoHostView hostView;

    public ReactVideoView(ThemedReactContext themedReactContext, ReactVideoHostView hostView) {
        super(themedReactContext);

        mThemedReactContext = themedReactContext;
        this.hostView = hostView;
        mEventEmitter = themedReactContext.getJSModule(RCTEventEmitter.class);

        initializeMediaPlayerIfNeeded();
        setSurfaceTextureListener(this);

        //TODO run while playing
        mProgressUpdateRunnable = new Runnable() {
            @Override
            public void run() {

                if (mMediaPlayerValid) {
                    WritableMap event = Arguments.createMap();
                    event.putDouble(EVENT_PROP_CURRENT_TIME, mMediaPlayer.getCurrentPosition() / 1000.0);
                    event.putDouble(EVENT_PROP_PLAYABLE_DURATION, mVideoBufferedDuration / 1000.0); //TODO:mBufferUpdateRunnable
                    mEventEmitter.receiveEvent(getContainerId(), Events.EVENT_PROGRESS.toString(), event);
                }
                mProgressUpdateHandler.postDelayed(mProgressUpdateRunnable, 250);
            }
        };
        mProgressUpdateHandler.post(mProgressUpdateRunnable);

        setOnSystemUiVisibilityChangeListener(this);
    }

    private void initializeMediaPlayerIfNeeded() {
        if (mMediaPlayer == null) {
            Log.d(ReactVideoViewManager.REACT_CLASS, "initializeMediaPlayerIfNeeded() doing init");
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

    public void  setSrc(final String uriString, final String type, final boolean isNetwork, final boolean isAsset) {
        Log.d(ReactVideoViewManager.REACT_CLASS, "setSrc() " + uriString);
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
        mEventEmitter.receiveEvent(getContainerId(), Events.EVENT_LOAD_START.toString(), event);

        prepareAsync(this);
    }

    private int getContainerId() {
        return hostView.getId();
    }

    public void setResizeModeModifier(final ScalableType resizeMode) {
        mResizeMode = resizeMode;

        if (mMediaPlayerValid) {
            setScalableType(resizeMode);
            requestLayout();
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
                mController = new MediaControllerView(mThemedReactContext);
                mController.setMediaPlayer(this);
                ViewParent parent = getParent();
                if (!(parent instanceof FrameLayout)) {
                    throw new IllegalStateException("Parent must be FrameLayout");
                }
                // Controller anchor is the parent frame layout
                mController.setAnchorView((ViewGroup)getParent());
                mController.setVisibilityListener(this);
                //mController.enableFullScreenButton(this.hostView.canGoFullScreen());
            }
            if (!mController.isShowing()) {
                showController();
            }
        } else if (mController != null) {
            mController.hide();
        }
    }

    /**
     * Shows with timeout if playing. Otherwise shows and persists.
     */
    private void showController() {
        if (mMediaPlayerValid && this.isPlaying()) {
            mController.show();
        } else {
            mController.show(0);
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
        Log.d(ReactVideoViewManager.REACT_CLASS, "onPrepared() ");
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
        mEventEmitter.receiveEvent(getContainerId(), Events.EVENT_LOAD.toString(), event);

        applyModifiers();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.d(ReactVideoViewManager.REACT_CLASS, "onError() " + what);
        WritableMap error = Arguments.createMap();
        error.putInt(EVENT_PROP_WHAT, what);
        error.putInt(EVENT_PROP_EXTRA, extra);
        WritableMap event = Arguments.createMap();
        event.putMap(EVENT_PROP_ERROR, error);
        mEventEmitter.receiveEvent(getContainerId(), Events.EVENT_ERROR.toString(), event);
        return true;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        //Log.d(ReactVideoViewManager.REACT_CLASS, "onBufferingUpdate() " + percent);
        mVideoBufferedDuration = (int) Math.round((double) (mVideoDuration * percent) / 100.0);
    }

    @Override
    public void seekTo(int msec) {

        if (mMediaPlayerValid) {
            WritableMap event = Arguments.createMap();
            event.putDouble(EVENT_PROP_CURRENT_TIME, getCurrentPosition() / 1000.0);
            event.putDouble(EVENT_PROP_SEEK_TIME, msec / 1000.0);
            mEventEmitter.receiveEvent(getContainerId(), Events.EVENT_SEEK.toString(), event);

            super.seekTo(msec);
        }
    }


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        super.onSurfaceTextureAvailable(surfaceTexture, width, height);
        Log.d(ReactVideoViewManager.REACT_CLASS, "onSurfaceTextureAvailable() " + width + "," + height);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        super.onSurfaceTextureSizeChanged(surface, width, height);
        Log.d(ReactVideoViewManager.REACT_CLASS, "onSurfaceTextureSizeChanged() " + width + "," + height);

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.d(ReactVideoViewManager.REACT_CLASS, "onSurfaceTextureDestroyed()");
        return super.onSurfaceTextureDestroyed(surface);
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        //Log.d(ReactVideoViewManager.REACT_CLASS, "onSurfaceTextureUpdated()");
        super.onSurfaceTextureUpdated(surface);
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        Log.d(ReactVideoViewManager.REACT_CLASS, "onVideoSizeChanged() " + width + "," + height);
        super.onVideoSizeChanged(mp, width, height);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(ReactVideoViewManager.REACT_CLASS, "onCompletion() ");
        //mMediaPlayerValid = false;
        mEventEmitter.receiveEvent(getContainerId(), Events.EVENT_END.toString(), null);
        if (mController != null) {
            mController.show(0);
        }
    }

    // Container should call this when window detaches
    public void doCleanup() {
        release();
        mMediaPlayerValid = false;
        if (mController != null) {
            // Media player is going away in base
            mController.setMediaPlayer(null);
            mController = null;
        }
    }

    // Container should call this when window attaches
    public void doInit() {
        setSrc(mSrcUriString, mSrcType, mSrcIsNetwork, mSrcIsAsset);
        setShowControls(mShowControls);
    }

    @Override
    protected void onDetachedFromWindow() {
        Log.d(ReactVideoViewManager.REACT_CLASS, "onDetachedFromWindow() ");
        super.onDetachedFromWindow();
    }

    @Override
    protected void onAttachedToWindow() {
        Log.d(ReactVideoViewManager.REACT_CLASS, "onAttachedToWindow() ");
        super.onAttachedToWindow();
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
                showController();
            }
        }
    }

    private void toggleMediaControllerVisibility() {
        if (mController.isShowing()) {
            mController.hide();
        } else {
            showController();
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


    @Override
    public boolean isFullScreen() {
        return hostView.isFullScreen();
    }

    @Override
    public void toggleFullScreen() {
        boolean changed = false;
        if (isFullScreen()) {
            changed = hostView.goEmbed();
        } else {
            changed = hostView.goFullScreen();
        }
        if (changed) {
            Events event = isFullScreen() ? Events.EVENT_ENTER_FS : Events.EVENT_EXIT_FS;
            mEventEmitter.receiveEvent(getContainerId(), event.toString(), null);
        }
    }

    public  int getAudioSessionId() {
        return mMediaPlayer.getAudioSessionId();
    }
}
