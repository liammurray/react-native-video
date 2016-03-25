package com.xealth.mediacontroller;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.xealth.mediacontroller.callback.Callback;
import com.xealth.mediacontroller.callback.WeakRefCallback;

import java.util.Formatter;
import java.util.Locale;

/**
 * Custom media contoller based on copy of Android MediaController class.
 * See: http://www.brightec.co.uk/ideas/custom-android-media-controller
 */
public class MediaControllerView extends LinearLayout {
    private static final String TAG = "VideoControllerView";

    private MediaPlayerControl mPlayer;
    private Context mContext;
    private ViewGroup mAnchor;

    private ProgressBar mProgress;
    private TextView mEndTime, mCurrentTime;
    private boolean mShowing;
    private boolean mDragging;
    private static final int sDefaultTimeout = 3000;
    private boolean mEnableForwardReverseButtons = false;
    private boolean mEnableFullScreenButton = true;
    private View.OnClickListener mNextListener, mPrevListener;
    private final StringBuilder mFormatBuilder = new StringBuilder();
    private Formatter mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
    private ImageButton mPlayPauseButton;
    private ImageButton mFfwdButton;
    private ImageButton mRewButton;
    private ImageButton mNextButton;
    private ImageButton mPrevButton;
    private ImageButton mFullscreenButton;
    private final Handler mHandler = new Handler();

    public MediaControllerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        //setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public MediaControllerView(Context context) {
        this(context, null);
    }

    @SuppressLint("WrongViewCast")
    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        mPlayPauseButton = (ImageButton) findViewById(R.id.pause);
        include(mPlayPauseButton, true, mPlayPauseListener);

        mFullscreenButton = (ImageButton) findViewById(R.id.fullscreen);
        include(mFullscreenButton, mEnableFullScreenButton, mFullscreenListener);

        mFfwdButton = (ImageButton) findViewById(R.id.ffwd);
        include(mFfwdButton, mEnableForwardReverseButtons, mFfwdListener);

        mRewButton = (ImageButton) findViewById(R.id.rew);
        include(mRewButton, mEnableForwardReverseButtons, mRewListener);

        // These are hidden if no listener
        mNextButton = (ImageButton) findViewById(R.id.next);
        include(mNextButton, mNextListener != null, mNextListener);
        mPrevButton = (ImageButton) findViewById(R.id.prev);
        include(mPrevButton, mPrevListener != null, mPrevListener);


        mProgress = (ProgressBar) findViewById(R.id.mediacontroller_progress);
        if (mProgress != null) {
            if (mProgress instanceof SeekBar) {
                SeekBar seeker = (SeekBar) mProgress;
                seeker.setOnSeekBarChangeListener(mSeekListener);
            }
            mProgress.setMax(1000);
        }

        mEndTime = (TextView) findViewById(R.id.time);
        mCurrentTime = (TextView) findViewById(R.id.time_current);
        setTextTime(mEndTime, 0);
        setTextTime(mCurrentTime, 0);

    }

    public void setMediaPlayer(MediaPlayerControl player) {
        mPlayer = player;
        updatePausePlayButtonState();
        updateFullScreenButtonState();
    }

    private static void detachFromParent(View view) {
        if (view != null) {
            ViewParent parent = view.getParent();
            if (parent != null) {
                ((ViewGroup) parent).removeView(view);
            }
        }
    }

    public void setShowTimeout(int timeout) {
        hideCallback.setTimeout(timeout);
    }

    /**
     * Set the FrameLayout that acts as the anchor for the control view
     *
     * @param view The view to which to anchor the controller when it is visible.
     */
    public void setAnchorView(FrameLayout view) {
        if (mAnchor == view) {
            // Already current anchor
            return;
        }
        mAnchor = view;
        detachFromParent(this);
        if (mAnchor != null) {
            // See if controller layout params provided in layout resource (hint: pass parent FrameLayout to inflater.inflate())
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) getLayoutParams();
            if (params == null) {
                // Default if none specified in layout
                params = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER
                );
            }
            mAnchor.addView(this, params);
        }
    }


    /**
     * Show the controller on screen. It will go away
     * automatically after N seconds of inactivity.
     */
    public void show() {
        show(false);
    }

    private static void enable(View view, boolean enable) {
        if (view != null) {
            view.setEnabled(enable);
        }
    }

    private static void include(View view, boolean show, View.OnClickListener listener) {
        if (view != null) {
            view.setVisibility(show ? View.VISIBLE : View.GONE);
            if (show) {
                view.setOnClickListener(listener);
            }
        }
    }

    private boolean isPlayerReady() {
        return mPlayer != null && mPlayer.canPlay();
    }

    private void syncEnabledStates() {
        boolean playerReady = isPlayerReady();
        enable(mFullscreenButton, playerReady && mPlayer.canGoFullScreen());
        enable(mPlayPauseButton, playerReady);
        enable(mRewButton, playerReady && mPlayer.canSeekBackward());
        enable(mFfwdButton, playerReady && mPlayer.canSeekForward());
        enable(mNextButton, playerReady && mNextListener != null);
        enable(mPrevButton, playerReady && mPrevListener != null);
        enable(mProgress, playerReady);

    }


    static final int FADE_ANIM_DURATION = 250;

    private static void hideView(final View view) {
        ObjectAnimator anim = ObjectAnimator.ofFloat(view, "alpha", 0);
        anim.setDuration(FADE_ANIM_DURATION);
        anim.addListener(new
                                 AnimatorListenerAdapter() {
                                     public void onAnimationEnd(Animator animation) {
                                         view.setAlpha(1);
                                         view.setVisibility(View.INVISIBLE);
                                     }
                                 }
        );
        anim.start();
    }

    private static void showView(View view) {
        ObjectAnimator anim = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        anim.setDuration(FADE_ANIM_DURATION);
        anim.start();
        view.setVisibility(View.VISIBLE);
    }


    /**
     * Show the controller on screen. It will go away
     * automatically after 'timeout' milliseconds of inactivity.
     */
    public void show(boolean persist) {
        //Log.d("RCTVideo", "MediaControllerView: show: " + timeout);
        if (!mShowing) {
            showView(this);
            mShowing = true;
            setProgress();
            if (mPlayPauseButton != null) {
                mPlayPauseButton.requestFocus();
            }
            syncEnabledStates();
            notifyVisibilityChanged(true);
        }
        updatePausePlayButtonState();
        updateFullScreenButtonState();

        if (persist) {
            hideCallback.cancel();
        } else {
            hideCallback.reset();
        }
        progressCallback.set();
    }

    public boolean isShowing() {
        return mShowing;
    }

    public interface VisibilityListener {
        void onControllerVisibilityChanged(boolean attached);
    }

    private VisibilityListener visibilityListener;


    public void setVisibilityListener(VisibilityListener listener) {
        visibilityListener = listener;
    }

    protected void notifyVisibilityChanged(boolean showing) {
        if (visibilityListener != null) {
            visibilityListener.onControllerVisibilityChanged(showing);
        }
    }


    private WeakRefCallback.DoRunnable hideRunnable = new WeakRefCallback.DoRunnable() {
        @Override
        public boolean doRun() {
            hide();
            return false;
        }
    };


    private WeakRefCallback.DoRunnable progressRunnable = new WeakRefCallback.DoRunnable() {
        @Override
        public boolean doRun() {
            setProgress();
            return (!mDragging && mShowing && mPlayer.isPlaying());
        }
    };

    private Callback hideCallback = new WeakRefCallback(mHandler, sDefaultTimeout, hideRunnable);
    private static final int PROGRESS_UPDATE_INTERVAL = 250;
    private Callback progressCallback = new WeakRefCallback(mHandler, PROGRESS_UPDATE_INTERVAL, progressRunnable);


    /**
     * Remove the controller from the screen.
     */
    public void hide() {
        if (mShowing) {
            hideView(this);
            progressCallback.cancel();
            hideCallback.cancel();
            mShowing = false;
            notifyVisibilityChanged(false);
        }

    }

    private String stringForTime(long timeMs) {
        long totalSeconds = timeMs / 1000;

        long seconds = totalSeconds % 60;
        long minutes = (totalSeconds / 60) % 60;
        long hours = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    private void setTextTime(TextView view, long time) {
        if (view != null) {
            view.setText(stringForTime(time));
        }
    }

    private long setProgress() {
        if (mPlayer == null || mDragging) {
            return 0;
        }
        long position = 0;
        long duration = 0;
        if (mPlayer.canPlay()) {
            position = mPlayer.getCurrentPosition();
            duration = mPlayer.getDuration();
        }
        if (mProgress != null) {
            long pos = (duration > 0) ? 1000L * position / duration : 0;
            mProgress.setProgress((int) pos);
            int percent = mPlayer.getBufferPercentage();
            long bufferDuration = (percent * (long) duration) / 100;
            mProgress.setSecondaryProgress((int) bufferDuration);
        }

        setTextTime(mCurrentTime, position);
        setTextTime(mEndTime, duration);

        return position;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d("RCTVideo", "MediaControllerView::onTouchEvent()");
        hideCallback.extend();
        return true;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        hideCallback.extend();
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mPlayer == null) {
            return true;
        }

        int keyCode = event.getKeyCode();

        // Taken from KeyCompatibleMediaController from ExoPlayer project
        if (mPlayer.canSeekForward() && keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                mPlayer.seekTo(mPlayer.getCurrentPosition() + 15000); // milliseconds
                hideCallback.extend();
            }
            return true;
        } else if (mPlayer.canSeekBackward() && keyCode == KeyEvent.KEYCODE_MEDIA_REWIND) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                mPlayer.seekTo(mPlayer.getCurrentPosition() - 5000); // milliseconds
                hideCallback.extend();
            }
            return true;
        }

        final boolean uniqueDown = event.getRepeatCount() == 0
                && event.getAction() == KeyEvent.ACTION_DOWN;
        if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || keyCode == KeyEvent.KEYCODE_SPACE) {
            if (uniqueDown) {
                doPauseResume();
                hideCallback.extend();
                if (mPlayPauseButton != null) {
                    mPlayPauseButton.requestFocus();
                }
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
            if (uniqueDown && !mPlayer.isPlaying()) {
                mPlayer.start();
                updatePausePlayButtonState();
                hideCallback.extend();
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            if (uniqueDown && mPlayer.isPlaying()) {
                mPlayer.pause();
                updatePausePlayButtonState();
                hideCallback.extend();
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP
                || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {
            // don't show the controls for volume adjustment
            return super.dispatchKeyEvent(event);
        } else if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
            Log.d("RCTVideo", "MediaControllerView::dispatchKeyEvent()");
            if (uniqueDown) {
                hide();
            }
            return true;
        }

        hideCallback.extend();
        return super.dispatchKeyEvent(event);
    }

    private View.OnClickListener mPlayPauseListener = new View.OnClickListener() {
        public void onClick(View v) {
            doPauseResume();
            hideCallback.extend();
        }
    };


    private View.OnClickListener mFullscreenListener = new View.OnClickListener() {
        public void onClick(View v) {
            doToggleFullscreen();
            hideCallback.extend();
        }
    };


    public void updatePausePlayButtonState() {
        if (mPlayPauseButton == null || mPlayer == null) {
            return;
        }

        if (mPlayer.isPlaying()) {
            mPlayPauseButton.setImageResource(R.drawable.ic_media_pause);
        } else {
            mPlayPauseButton.setImageResource(R.drawable.ic_media_play);
        }
    }

    public void updateFullScreenButtonState() {
        if (mFullscreenButton == null || mPlayer == null) {
            return;
        }

        if (mPlayer.isFullScreen()) {
            mFullscreenButton.setImageResource(R.drawable.ic_media_fullscreen_shrink);
        } else {
            mFullscreenButton.setImageResource(R.drawable.ic_media_fullscreen_stretch);
        }
    }

    private void doPauseResume() {
        if (mPlayer == null) {
            return;
        }

        if (mPlayer.isPlaying()) {
            mPlayer.pause();
        } else {
            mPlayer.start();
        }
        updatePausePlayButtonState();
    }

    private void doToggleFullscreen() {
        if (mPlayer == null) {
            return;
        }

        mPlayer.toggleFullScreen();
    }

    // There are two scenarios that can trigger the seekbar listener to trigger:
    //
    // The first is the user using the touchpad to adjust the posititon of the
    // seekbar's thumb. In this case onStartTrackingTouch is called followed by
    // a number of onProgressChanged notifications, concluded by onStopTrackingTouch.
    // We're setting the field "mDragging" to true for the duration of the dragging
    // session to avoid jumps in the position in case of ongoing playback.
    //
    // The second scenario involves the user operating the scroll ball, in this
    // case there WON'T BE onStartTrackingTouch/onStopTrackingTouch notifications,
    // we will simply apply the updated position without suspending regular updates.
    private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
        public void onStartTrackingTouch(SeekBar bar) {
            show(true);
            mDragging = true;
            progressCallback.cancel();
        }

        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (mPlayer == null) {
                return;
            }

            if (!fromuser) {
                // We're not interested in programmatically generated changes to
                // the progress bar's position.
                return;
            }

            long duration = mPlayer.getDuration();
            long newposition = (duration * progress) / 1000L;
            mPlayer.seekTo((int) newposition);
            if (mCurrentTime != null)
                mCurrentTime.setText(stringForTime((int) newposition));
        }

        public void onStopTrackingTouch(SeekBar bar) {
            mDragging = false;
            setProgress();
            updatePausePlayButtonState();
            show();
            progressCallback.set();
        }
    };

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (enabled) {
            syncEnabledStates();
        }
    }

    private View.OnClickListener mRewListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mPlayer == null) {
                return;
            }

            long pos = mPlayer.getCurrentPosition();
            pos -= 5000; // milliseconds
            mPlayer.seekTo(pos);
            setProgress();

            hideCallback.extend();
        }
    };

    private View.OnClickListener mFfwdListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mPlayer == null) {
                return;
            }

            long pos = mPlayer.getCurrentPosition();
            pos += 15000; // milliseconds
            mPlayer.seekTo(pos);
            setProgress();

            hideCallback.extend();
        }
    };


    public void setPrevNextListeners(View.OnClickListener next, View.OnClickListener prev) {
        mNextListener = next;
        mPrevListener = prev;
        include(mNextButton, mNextListener != null, mNextListener);
        include(mPrevButton, mPrevListener != null, mPrevListener);
    }

    public interface MediaPlayerControl {
        void start();

        void pause();

        long getDuration();

        long getCurrentPosition();

        void seekTo(long pos);

        boolean isPlaying();

        int getBufferPercentage();

        long getBufferDuration();

        boolean canPlay();

        boolean canSeekBackward();

        boolean canSeekForward();

        boolean canGoFullScreen();

        boolean isFullScreen();

        void toggleFullScreen();
    }


    public void onPlay() {
        updatePausePlayButtonState();
        show();
    }

    public void onPause() {
        updatePausePlayButtonState();
        show(true);
    }

    public void onStop() {
        updatePausePlayButtonState();
        show(true);
    }

    public void onLoad() {
        updatePausePlayButtonState();
    }

    public void onFullScreenSwitch() {
        updateFullScreenButtonState();
    }

    public void onPlayerReady() {
        this.syncEnabledStates();
    }
}
