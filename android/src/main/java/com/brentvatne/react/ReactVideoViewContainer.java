package com.brentvatne.react;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.brentvatne.RCTVideo.R;
import com.brentvatne.react.ReactVideo.Events;
import com.brentvatne.react.com.brentvatne.react.exoplayer.ExoPlayerWrapper;
import com.brentvatne.react.com.brentvatne.react.exoplayer.ReactVideoExoView;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.xealth.mediacontroller.MediaControllerView;

import static com.brentvatne.react.ReactVideo.EVENT_PROP_CURRENT_TIME;
import static com.brentvatne.react.ReactVideo.EVENT_PROP_DURATION;
import static com.brentvatne.react.ReactVideo.EVENT_PROP_ERROR;
import static com.brentvatne.react.ReactVideo.EVENT_PROP_EXTRA;
import static com.brentvatne.react.ReactVideo.EVENT_PROP_FAST_FORWARD;
import static com.brentvatne.react.ReactVideo.EVENT_PROP_PLAYABLE_DURATION;
import static com.brentvatne.react.ReactVideo.EVENT_PROP_REVERSE;
import static com.brentvatne.react.ReactVideo.EVENT_PROP_SEEK_TIME;
import static com.brentvatne.react.ReactVideo.EVENT_PROP_SLOW_FORWARD;
import static com.brentvatne.react.ReactVideo.EVENT_PROP_SLOW_REVERSE;
import static com.brentvatne.react.ReactVideo.EVENT_PROP_STEP_BACKWARD;
import static com.brentvatne.react.ReactVideo.EVENT_PROP_STEP_FORWARD;
import static com.brentvatne.react.ReactVideo.EVENT_PROP_WHAT;

/**
 * Frame that parents video view and media transport controller (player controls)
 */
public class ReactVideoViewContainer extends FrameLayout implements View.OnSystemUiVisibilityChangeListener,
        PlayerEventListener, MediaControllerView.VisibilityListener {

    private ReactVideoHostView mHostView;

    private ReactVideoExoView mVideoView;

    private MediaControllerView mController;

    private boolean mShowControls = true;

    private boolean mAutoHideNav = true;

    private int mLastSystemUiVis;

    private RCTEventEmitter mEventEmitter;

    private InfoView infoView;

    private MediaControllerView.MediaPlayerControl playerControl = createPlayerControl();


    public ReactVideoViewContainer(ThemedReactContext context, ReactVideoHostView hostView) {
        super(context);
        mHostView = hostView;

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mVideoView = (ReactVideoExoView)inflater.inflate(R.layout.exo_video_view, this, false);
        playerControl = createPlayerControl();
        mVideoView.setEventListener(this);
        addView(mVideoView, newFrameLayoutParams());

        infoView = (InfoView)inflater.inflate(R.layout.info_view, this, false);
        addView(infoView);
        infoView.setState(InfoView.State.HIDDEN);

        //setBackground(Color.BLACK);
        mEventEmitter = context.getJSModule(RCTEventEmitter.class);
        setOnSystemUiVisibilityChangeListener(this);
    }

    private FrameLayout.LayoutParams newFrameLayoutParams() {
        return new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
    }

    public void setAutoHideNav(final boolean autoHideNav) {
        mAutoHideNav = autoHideNav;
    }

    private void ensureController() {
        if (mController != null) {
            return;
        }
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // Inflate creating correct version of layout params based on layout
        mController = (MediaControllerView)inflater.inflate(R.layout.media_controller, this, false);
        mController.setAnchorView(this);

    }



    /** Enables or disables controller */
    public void setShowControls(final boolean showControls) {
        Log.d(ReactVideoViewManager.REACT_CLASS, "Container.setShowControls(): " + showControls);
        mShowControls = showControls;
        if (mShowControls) {
            ensureController();
            if (!mController.isShowing()) {
                showController();
            }
            mController.setAnchorView(this);

            mController.setMediaPlayer(playerControl);
            mController.setVisibilityListener(this);
        } else if (mController != null) {
            mController.hide();
            mController = null;
        }
    }

    @Override
    public void onSystemUiVisibilityChange(int visibility) {
        Log.d(ReactVideoViewManager.REACT_CLASS, "Container.onSystemUiVisibilityChange(): " + visibility);
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
    public void onControllerVisibilityChanged(boolean attached) {
        //Log.d(ReactVideoViewManager.REACT_CLASS, "Container.onControllerVisibilityChanged(): autohide nav:" + mAutoHideNav);
        if (mAutoHideNav) {
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
        if (mController != null) {
            // In case where we set nav visibility outside of listener that observes media controller visibility changes
            setMediaControllerVisibility(visible);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            Log.d(ReactVideoViewManager.REACT_CLASS, "Container.onTouchEvent(): DOWN");
            // First finger down
            if (mController != null) {
                if (mVideoView.isPlaying() || !playerControl.canPlay()) {
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
        if (mVideoView.isPlaying()) {
            playerControl.pause();
        } else {
            playerControl.start();
        }
    }

    /** Shows or hides controller */
    private void toggleController() {
        if (mController.isShowing()) {
            mController.hide();
        } else {
            showController();
        }
    }


    MediaControllerView.MediaPlayerControl createPlayerControl() {
        return  new MediaControllerView.MediaPlayerControl() {

            @Override
            public void start() {
                mVideoView.setPaused(false);
            }

            @Override
            public void pause() {
                mVideoView.setPaused(true);
            }

            @Override
            public long getDuration() {
                return mVideoView.getDuration();
            }

            @Override
            public long getCurrentPosition() {
                return mVideoView.getCurrentPosition();
            }

            @Override
            public void seekTo(long pos) {
                mVideoView.seekTo(pos);
            }

            @Override
            public boolean isPlaying() {
                return mVideoView.isPlaying();
            }

            @Override
            public int getBufferPercentage() {
                return mVideoView.getBufferedPercentage();
            }

            @Override
            public long getBufferDuration() {
                return mVideoView.getBufferedDuration();
            }

            @Override
            public boolean canPlay() {
                return mVideoView.canPlay();
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
                return mHostView.canGoFullScreen();
            }

            @Override
            public boolean isFullScreen() {
                return mHostView.isFullScreen();
            }

            @Override
            public void toggleFullScreen() {
                doFullScreenToggle();
            }
        };
    }


    /** Id for React view */
    private int getHostViewId() {
        return mHostView.getId();
    }

    public void doFullScreenToggle() {
        if (mHostView.isFullScreen()) {
            mHostView.goEmbed();
        } else {
            mHostView.goFullScreen();
        }
    }

    /**
     * Called when host view attaches (but not when this window attaches during fs switch)
     */
    public void doInit() {
        Log.d(ReactVideoViewManager.REACT_CLASS, "Container.doInit()");
        mVideoView.init();
        setShowControls(mShowControls);
    }

    /**
     * Called when host view detaches (but not when this window detaches during fs switch)
     */
    public void doCleanup() {
        Log.d(ReactVideoViewManager.REACT_CLASS, "Container.doCleanup()");
        mVideoView.cleanUp(true);
        if (mController != null) {
            // Since we disable callbacks we can't rely on stop callbacks
            mController.onStop();
            mController.hide();
            // Disable callbacks until next init
            mController.setMediaPlayer(null);
            mController.setVisibilityListener(null);
        }
    }

    /**
     * Shows with timeout if playing. Otherwise shows and persists.
     */
    private void showController() {
        boolean persist = !playerControl.isPlaying();
        Log.d(ReactVideoViewManager.REACT_CLASS, "Container.showController(): persist: " + persist);
        mController.show(persist);
    }


    private void setMediaControllerVisibility(boolean setVisible) {
        Log.d(ReactVideoViewManager.REACT_CLASS, "Container.setMediaControllerVisibility()");
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

    public void onFullScreenSwitch() {
        if (mController != null) {
            mController.onFullScreenSwitch();
        }
        Events event = playerControl.isFullScreen() ? Events.EVENT_ENTER_FS : Events.EVENT_EXIT_FS;
        mEventEmitter.receiveEvent(getHostViewId(), event.toString(), null);
    }

    @Override
    public void onPause() {
        Log.d(ReactVideoViewManager.REACT_CLASS, "Container.onPause()");
        if (mController != null) {
            mController.onPause();
        }
    }

    @Override
    public void onStop() {
        Log.d(ReactVideoViewManager.REACT_CLASS, "Container.onStop()");
        mEventEmitter.receiveEvent(getHostViewId(), ReactVideo.Events.EVENT_END.toString(), null);
        if (mController != null) {
            mController.onStop();
        }
    }

    @Override
    public void onProgress(long currentPos) {
        WritableMap event = Arguments.createMap();
        event.putDouble(EVENT_PROP_CURRENT_TIME, currentPos / 1000.0);
        event.putDouble(EVENT_PROP_PLAYABLE_DURATION, playerControl.getBufferDuration() / 1000.0);
        mEventEmitter.receiveEvent(getHostViewId(), Events.EVENT_PROGRESS.toString(), event);
    }

    @Override
    public void onBuffer(int percent, long duration) {
        Log.d(ReactVideoViewManager.REACT_CLASS, "Container.onBuffer(): " + percent);
        infoView.setState(InfoView.State.HIDDEN);
        WritableMap event = Arguments.createMap();
        event.putDouble(EVENT_PROP_PLAYABLE_DURATION, playerControl.getBufferDuration() / 1000.0);
        mEventEmitter.receiveEvent(getHostViewId(), Events.EVENT_PROGRESS.toString(), event);

    }

    @Override
    public void onSeek(long destPos, long currentPos) {
        WritableMap event = Arguments.createMap();
        event.putDouble(EVENT_PROP_CURRENT_TIME, currentPos / 1000.0);
        event.putDouble(EVENT_PROP_SEEK_TIME, destPos / 1000.0);
        mEventEmitter.receiveEvent(getHostViewId(), Events.EVENT_SEEK.toString(), event);

    }


    @Override
    public void onError(Exception e, boolean isFatal) {
        Log.d(ReactVideoViewManager.REACT_CLASS, "Container.onError(): " + e.getMessage());
        //TODO Show other errors?
        infoView.setState(InfoView.State.HIDDEN);
        if (isFatal) {
            infoView.setState(InfoView.State.FAILED, e.getMessage());
            if (mController != null) {
                mController.onStop();
            }
        }
        WritableMap error = Arguments.createMap();
        error.putInt(EVENT_PROP_WHAT, 0); //TODO
        error.putString(EVENT_PROP_EXTRA, e.getMessage());
        WritableMap event = Arguments.createMap();
        event.putMap(EVENT_PROP_ERROR, error);
        mEventEmitter.receiveEvent(getHostViewId(), Events.EVENT_ERROR.toString(), event);
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
        if (mController != null) {
            mController.onLoad();
        }
        infoView.setState(InfoView.State.LOADING);
        WritableMap src = Arguments.createMap();
        src.putString(ReactVideoViewManager.PROP_SRC_URI, uriString);
        src.putString(ReactVideoViewManager.PROP_SRC_TYPE, type);
        src.putBoolean(ReactVideoViewManager.PROP_SRC_IS_NETWORK, isNetwork);
        WritableMap event = Arguments.createMap();
        event.putMap(ReactVideoViewManager.PROP_SRC, src);
        mEventEmitter.receiveEvent(getHostViewId(), Events.EVENT_LOAD_START.toString(), event);
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
        mEventEmitter.receiveEvent(getHostViewId(), Events.EVENT_LOAD.toString(), event);
        if (mController != null) {
            mController.onPlayerReady();
        }
    }

    @Override
    public void onPlay() {
        Log.d(ReactVideoViewManager.REACT_CLASS, "Container.onPlay()");
        infoView.setState(InfoView.State.HIDDEN);
        if (mController != null) {
            mController.onPlay();
        }
    }

    public ReactVideoExoView getVideoView() {
        return mVideoView;
    }

}




