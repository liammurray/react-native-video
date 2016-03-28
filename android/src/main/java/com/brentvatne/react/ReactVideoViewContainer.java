package com.brentvatne.react;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.brentvatne.RCTVideo.R;
import com.brentvatne.react.exoplayer.ExoPlayerView;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.xealth.mediacontroller.MediaControllerView;

/**
 * Frame that parents the actual video view and media transport controller (player controls).
 */
public class ReactVideoViewContainer extends FrameLayout implements View.OnSystemUiVisibilityChangeListener,
        PlayerEventListener, MediaControllerView.VisibilityListener {

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

    /** Parent view in embedded mode */
    private ReactVideoHostView hostView;

    /** Actual video view */
    private ExoPlayerView videoView;

    /** Player transport controls */
    private MediaControllerView controller;

    /** View that covers player view while loading or presenting error state message */
    private InfoView infoView;

    private boolean enableControllerView = true;

    /** Auto hide navigation bar when controller view goes away? */
    private boolean autoHideNav = true;

    private int lastSystemUiVis;

    private RCTEventEmitter eventEmitter;

    private MediaControllerView.MediaPlayerControl playerControl = createPlayerControl();

    public ReactVideoViewContainer(ThemedReactContext context, ReactVideoHostView hostView) {
        super(context);
        this.hostView = hostView;

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        videoView = (ExoPlayerView)inflater.inflate(R.layout.exo_video_view, this, false);
        playerControl = createPlayerControl();
        videoView.setEventListener(this);
        addView(videoView, newFrameLayoutParams());

        infoView = (InfoView)inflater.inflate(R.layout.info_view, this, false);
        addView(infoView);
        infoView.setState(InfoView.State.HIDDEN);

        eventEmitter = context.getJSModule(RCTEventEmitter.class);
        setOnSystemUiVisibilityChangeListener(this);
    }

    private FrameLayout.LayoutParams newFrameLayoutParams() {
        return new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
    }

    public void setAutoHideNav(final boolean autoHideNav) {
        this.autoHideNav = autoHideNav;
    }

    private void ensureController() {
        if (controller != null) {
            return;
        }
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // Inflate creating correct version of layout params based on layout
        controller = (MediaControllerView)inflater.inflate(R.layout.media_controller, this, false);
        controller.setAnchorView(this);

    }



    /** Enables or disables controller */
    public void setEnableControllerView(final boolean enableControllerView) {
        Log.d(ReactVideoViewManager.REACT_CLASS, "Container.setEnableControllerView(): " + enableControllerView);
        this.enableControllerView = enableControllerView;
        if (this.enableControllerView) {
            ensureController();
            if (!controller.isShowing()) {
                showController();
            }
            controller.setAnchorView(this);

            controller.setMediaPlayer(playerControl);
            controller.setVisibilityListener(this);
        } else if (controller != null) {
            controller.hide();
            controller = null;
        }
    }

    @Override
    public void onSystemUiVisibilityChange(int visibility) {
        Log.d(ReactVideoViewManager.REACT_CLASS, "Container.onSystemUiVisibilityChange(): " + visibility);
        int diff = lastSystemUiVis ^ visibility;
        lastSystemUiVis = visibility;
        if (autoHideNav) {
            // If SYSTEM_UI_FLAG_HIDE_NAVIGATION removed from visibility flags...
            if ((diff & SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0
                    && (visibility & SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
                setNavVisibility(true);
            }
        }
    }

    @Override
    public void onControllerVisibilityChanged(boolean attached) {
        //Log.d(ReactVideoViewManager.REACT_CLASS, "Container.onControllerVisibilityChanged(): autohide nav:" + autoHideNav);
        if (autoHideNav) {
            setNavVisibility(attached);
        }
    }

    private void setNavVisibility(boolean visible) {
        Log.d(ReactVideoViewManager.REACT_CLASS, "Container.setNavVisibility(): " + visible);
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
        if (controller != null) {
            // In case where we set nav visibility outside of listener that observes media controller visibility changes
            setMediaControllerVisibility(visible);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            Log.d(ReactVideoViewManager.REACT_CLASS, "Container.onTouchEvent(): DOWN");
            // First finger down
            if (controller != null) {
                if (videoView.isPlaying() || !playerControl.canPlay()) {
                    // While playing (or in non-playable state) tapping on video toggles controller.
                    toggleController();
                } else {
                    // While stopped or paused tapping resumes or starts playback.
                    playerControl.start();
                    showController();
                }
            } else if (playerControl.canPlay()) {
                // No controller. Tap is play pause.
                togglePlayPause();
            }
        }
        return true;
    }

    private void togglePlayPause() {
        if (videoView.isPlaying()) {
            playerControl.pause();
        } else {
            playerControl.start();
        }
    }

    /** Shows or hides controller */
    private void toggleController() {
        if (controller.isShowing()) {
            controller.hide();
        } else {
            showController();
        }
    }


    MediaControllerView.MediaPlayerControl createPlayerControl() {
        return  new MediaControllerView.MediaPlayerControl() {

            @Override
            public void start() {
                videoView.setPaused(false);
            }

            @Override
            public void pause() {
                videoView.setPaused(true);
            }

            @Override
            public long getDuration() {
                return videoView.getDuration();
            }

            @Override
            public long getCurrentPosition() {
                return videoView.getCurrentPosition();
            }

            @Override
            public void seekTo(long pos) {
                videoView.seekTo(pos);
            }

            @Override
            public boolean isPlaying() {
                return videoView.isPlaying();
            }

            @Override
            public int getBufferPercentage() {
                return videoView.getBufferedPercentage();
            }

            @Override
            public long getBufferDuration() {
                return videoView.getBufferedDuration();
            }

            @Override
            public boolean canPlay() {
                return videoView.canPlay();
            }

            @Override
            public boolean canSeekBackward() {
                return true;
            }

            @Override
            public boolean canSeekForward() {
                return true;
            }

            @Override
            public boolean canGoFullScreen() {
                return hostView.canGoFullScreen();
            }

            @Override
            public boolean isFullScreen() {
                return hostView.isFullScreen();
            }

            @Override
            public void toggleFullScreen() {
                doFullScreenToggle();
            }
        };
    }


    /** Id for React view */
    private int getHostViewId() {
        return hostView.getId();
    }

    public void doFullScreenToggle() {
        if (hostView.isFullScreen()) {
            hostView.goEmbed();
        } else {
            hostView.goFullScreen();
        }
    }

    /**
     * Called when host view attaches (but not when this window attaches during fs switch)
     */
    public void doInit() {
        Log.d(ReactVideoViewManager.REACT_CLASS, "Container.doInit()");
        videoView.init();
        setEnableControllerView(enableControllerView);
    }

    /**
     * Called when host view detaches (but not when this window detaches during fs switch)
     */
    public void doCleanup() {
        Log.d(ReactVideoViewManager.REACT_CLASS, "Container.doCleanup()");
        videoView.cleanUp(true);
        if (controller != null) {
            // Since we disable callbacks we can't rely on stop callbacks
            controller.onStop();
            controller.hide();
            // Disable callbacks until next init
            controller.setMediaPlayer(null);
            controller.setVisibilityListener(null);
        }
    }

    /**
     * Shows with timeout if playing. Otherwise shows and persists.
     */
    private void showController() {
        boolean persist = !playerControl.isPlaying();
        Log.d(ReactVideoViewManager.REACT_CLASS, "Container.showController(): persist: " + persist);
        controller.show(persist);
    }


    private void setMediaControllerVisibility(boolean setVisible) {
        Log.d(ReactVideoViewManager.REACT_CLASS, "Container.setMediaControllerVisibility()");
        if (controller.isShowing()) {
            if (!setVisible) {
                controller.hide();
            }
        } else {
            if (setVisible) {
                showController();
            }
        }
    }

    public void onFullScreenSwitch() {
        if (controller != null) {
            controller.onFullScreenSwitch();
        }
        Events event = playerControl.isFullScreen() ? Events.EVENT_ENTER_FS : Events.EVENT_EXIT_FS;
        eventEmitter.receiveEvent(getHostViewId(), event.toString(), null);
    }

    @Override
    public void onPause() {
        Log.d(ReactVideoViewManager.REACT_CLASS, "Container.onPause()");
        if (controller != null) {
            controller.onPause();
        }
    }

    @Override
    public void onStop() {
        Log.d(ReactVideoViewManager.REACT_CLASS, "Container.onStop()");
        eventEmitter.receiveEvent(getHostViewId(), Events.EVENT_END.toString(), null);
        if (controller != null) {
            controller.onStop();
        }
    }

    @Override
    public void onProgress(long currentPos) {
        WritableMap event = Arguments.createMap();
        event.putDouble(EVENT_PROP_CURRENT_TIME, currentPos / 1000.0);
        event.putDouble(EVENT_PROP_PLAYABLE_DURATION, playerControl.getBufferDuration() / 1000.0);
        eventEmitter.receiveEvent(getHostViewId(), Events.EVENT_PROGRESS.toString(), event);
    }

    @Override
    public void onBuffer(int percent, long duration) {
        Log.d(ReactVideoViewManager.REACT_CLASS, "Container.onBuffer(): " + percent);
        infoView.setState(InfoView.State.HIDDEN);
        WritableMap event = Arguments.createMap();
        event.putDouble(EVENT_PROP_PLAYABLE_DURATION, playerControl.getBufferDuration() / 1000.0);
        eventEmitter.receiveEvent(getHostViewId(), Events.EVENT_PROGRESS.toString(), event);

    }

    @Override
    public void onSeek(long destPos, long currentPos) {
        WritableMap event = Arguments.createMap();
        event.putDouble(EVENT_PROP_CURRENT_TIME, currentPos / 1000.0);
        event.putDouble(EVENT_PROP_SEEK_TIME, destPos / 1000.0);
        eventEmitter.receiveEvent(getHostViewId(), Events.EVENT_SEEK.toString(), event);

    }


    @Override
    public void onError(Exception e, boolean isFatal) {
        Log.d(ReactVideoViewManager.REACT_CLASS, "Container.onError(): " + e.getMessage());
        //TODO Show other errors?
        infoView.setState(InfoView.State.HIDDEN);
        if (isFatal) {
            infoView.setState(InfoView.State.FAILED, e.getMessage());
            if (controller != null) {
                controller.onStop();
            }
        }
        WritableMap error = Arguments.createMap();
        error.putInt(EVENT_PROP_WHAT, 0); //TODO
        error.putString(EVENT_PROP_EXTRA, e.getMessage());
        WritableMap event = Arguments.createMap();
        event.putMap(EVENT_PROP_ERROR, error);
        eventEmitter.receiveEvent(getHostViewId(), Events.EVENT_ERROR.toString(), event);
    }

    @Override
    public void onVolume(float leftVolume, float rightVolume) {

    }

    @Override
    public void onMute(boolean muted) {

    }

    @Override
    public void onLoad(String uriString, String type, boolean isNetwork) {
        Log.d(ReactVideoViewManager.REACT_CLASS, "Container.onLoad()");
        if (controller != null) {
            controller.onLoad();
        }
        infoView.setState(InfoView.State.LOADING);
        WritableMap src = Arguments.createMap();
        src.putString(ReactVideoViewManager.PROP_SRC_URI, uriString);
        src.putString(ReactVideoViewManager.PROP_SRC_TYPE, type);
        src.putBoolean(ReactVideoViewManager.PROP_SRC_IS_NETWORK, isNetwork);
        WritableMap event = Arguments.createMap();
        event.putMap(ReactVideoViewManager.PROP_SRC, src);
        eventEmitter.receiveEvent(getHostViewId(), Events.EVENT_LOAD_START.toString(), event);
    }

    @Override
    public void onLoadComplete(long currentPosition, long videoDuration) {
        Log.d(ReactVideoViewManager.REACT_CLASS, "Container.onLoadComplete()");
        infoView.setState(InfoView.State.HIDDEN);
        WritableMap event = Arguments.createMap();
        event.putDouble(EVENT_PROP_DURATION, videoDuration / 1000.0);
        event.putDouble(EVENT_PROP_CURRENT_TIME, currentPosition / 1000.0);
        // TODO: Actually check if you can.
        event.putBoolean(EVENT_PROP_FAST_FORWARD, true);
        event.putBoolean(EVENT_PROP_SLOW_FORWARD, true);
        event.putBoolean(EVENT_PROP_SLOW_REVERSE, true);
        event.putBoolean(EVENT_PROP_REVERSE, true);
        event.putBoolean(EVENT_PROP_FAST_FORWARD, true);
        event.putBoolean(EVENT_PROP_STEP_BACKWARD, true);
        event.putBoolean(EVENT_PROP_STEP_FORWARD, true);
        eventEmitter.receiveEvent(getHostViewId(), Events.EVENT_LOAD.toString(), event);
        if (controller != null) {
            controller.onPlayerReady();
        }
    }

    @Override
    public void onPlay() {
        Log.d(ReactVideoViewManager.REACT_CLASS, "Container.onPlay()");
        infoView.setState(InfoView.State.HIDDEN);
        if (controller != null) {
            controller.onPlay();
        }
    }

    public ExoPlayerView getVideoView() {
        return videoView;
    }

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
}




