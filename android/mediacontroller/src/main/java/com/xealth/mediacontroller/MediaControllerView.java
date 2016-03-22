package com.xealth.mediacontroller;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import java.lang.ref.WeakReference;
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
    private static final int FADE_OUT = 1;
    private static final int SHOW_PROGRESS = 2;
    private boolean mEnableForwardReverseButtons = false;
    private boolean mEnableFullScreenButton = true;
    private boolean mListenersSet;
    private View.OnClickListener mNextListener, mPrevListener;
    StringBuilder mFormatBuilder;
    Formatter mFormatter;
    private ImageButton mPlayPauseButton;
    private ImageButton mFfwdButton;
    private ImageButton mRewButton;
    private ImageButton mNextButton;
    private ImageButton mPrevButton;
    private ImageButton mFullscreenButton;
    private Handler mHandler = new MessageHandler(this);

    public MediaControllerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
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
        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
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
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM
            );
            mAnchor.addView(this, params);
        }
    }


    /**
     * Show the controller on screen. It will go away
     * automatically after 3 seconds of inactivity.
     */
    public void show() {
        show(sDefaultTimeout);
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

    private void syncButtonEnabledStates() {
        if (mPlayer != null) {
            enable(mFullscreenButton, mPlayer.canGoFullScreen());
            enable(mPlayPauseButton, mPlayer.canPlay());
            enable(mRewButton, mPlayer.canSeekBackward());
            enable(mFfwdButton, mPlayer.canSeekForward());
        }
        enable(mNextButton, mNextListener != null);
        enable(mPrevButton, mPrevListener != null);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        logMeasureInfo(getSuggestedMinimumWidth(), getSuggestedMinimumHeight(), widthMeasureSpec, heightMeasureSpec);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Log.d("RCTVideo", "MediaControllerView: measured: w: " + getMeasuredWidth() + "; h: " + getMeasuredHeight());
    }

    private static void logMeasureInfo(int minWidth,
                                       int minHeight, int widthMeasureSpec,
                                       int heightMeasureSpec) {
        final int specWidth = MeasureSpec.getSize(widthMeasureSpec);
        final int specHeight = MeasureSpec.getSize(heightMeasureSpec);
        Log.d("RCTVideo", "MediaControllerView: minw: " + minWidth + "; minh: " + minHeight + "; sw: " + specWidth + "; sh: " + specHeight);
    }

    private void extendTimeout() {
        if (mHandler.hasMessages(FADE_OUT)) {
            show();
        }
    }

    static final int FADE_ANIM_DURATION = 250;

    private static void hideView(final View view) {
        ObjectAnimator anim = ObjectAnimator.ofFloat(view, "alpha", 0);
        anim.setDuration(FADE_ANIM_DURATION);
        anim.addListener(new
        AnimatorListenerAdapter() {
            public void onAnimationEnd (Animator animation){
                view.setAlpha(1);
                view.setVisibility(View.INVISIBLE);
            }
        }
        );
        anim.start();
    }

    private static void showView(View view){
        ObjectAnimator anim = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        anim.setDuration(FADE_ANIM_DURATION);
        anim.start();
        view.setVisibility(View.VISIBLE);
    }


    /**
     * Show the controller on screen. It will go away
     * automatically after 'timeout' milliseconds of inactivity.
     * @param timeout The timeout in milliseconds. Use 0 to show
     * the controller until hide() is called.
     */
    public void show(int timeout) {
        //Log.d("RCTVideo", "MediaControllerView: show: " + timeout);
        if (!mShowing) {
            showView(this);
            mShowing = true;
            setProgress();
            if (mPlayPauseButton != null) {
                mPlayPauseButton.requestFocus();
            }
            syncButtonEnabledStates();
            notifyVisibilityChanged(true);
        }
        updatePausePlayButtonState();
        updateFullScreenButtonState();

        //mHandler.removeMessages(SHOW_PROGRESS);
        mHandler.removeMessages(FADE_OUT);
        mHandler.sendEmptyMessage(SHOW_PROGRESS);
        if (timeout != 0) {
            Message msg = mHandler.obtainMessage(FADE_OUT);
            mHandler.sendMessageDelayed(msg, timeout);
        }
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

    abstract class SimpleCallback implements Runnable {

        private boolean isPending;
        private int timeout;

        public SimpleCallback(int timeout) {
            this.timeout = timeout;
        }

        public boolean isPending() {
            return isPending;
        }

        public void set() {
            if (!isPending) {
                extend();
            }
        }

        public void extend() {
            if (isPending) {
                mHandler.removeCallbacks(this);
            }
            mHandler.postDelayed(this, timeout);
            isPending = true;
        }

        public void cancel() {
            if (isPending) {
                mHandler.removeCallbacks(this);
            }
        }

        abstract boolean doRun();

        @Override
        public final void run() {
            isPending = false;
            isPending = doRun();
        }
    }

    SimpleCallback hideCallback = new SimpleCallback(sDefaultTimeout) {

        @Override
        public boolean doRun() {
            hide();
            return false;
        }

    };

    SimpleCallback progressCallback = new SimpleCallback(sDefaultTimeout) {
        private static final int UPDATE_INTERVAL = 250;
        @Override
        public boolean doRun() {
            int pos = setProgress();
            if (!mDragging && mShowing && mPlayer.isPlaying()) {
                mHandler.postDelayed(this, UPDATE_INTERVAL - (pos % UPDATE_INTERVAL));
                return true;
            }
            return false;
        }
    };

    /**
     * Remove the controller from the screen.
     */
    public void hide() {
        if (mShowing) {
            hideView(this);
            mHandler.removeMessages(SHOW_PROGRESS);
            mShowing = false;
            notifyVisibilityChanged(false);
        }

    }

    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours   = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    private int setProgress() {
        if (mPlayer == null || mDragging) {
            return 0;
        }
        int position = 0;
        int duration = 0;
        if (mPlayer.canPlay()) {
            position = mPlayer.getCurrentPosition();
            duration = mPlayer.getDuration();
        }
        if (mProgress != null) {
            // use long to avoid overflow
            long pos = (duration > 0) ? 1000L * position / duration : 0;
            mProgress.setProgress( (int) pos);
            int percent = mPlayer.getBufferPercentage();
            mProgress.setSecondaryProgress(percent * 10);
        }

        if (mEndTime != null) {
            mEndTime.setText(stringForTime(duration));
        }
        if (mCurrentTime != null) {
            mCurrentTime.setText(stringForTime(position));
        }

        return position;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d("RCTVideo", "MediaControllerView::onTouchEvent()");
        extendTimeout();
        return true;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        extendTimeout();
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mPlayer == null) {
            return true;
        }

        int keyCode = event.getKeyCode();
        final boolean uniqueDown = event.getRepeatCount() == 0
                && event.getAction() == KeyEvent.ACTION_DOWN;
        if (keyCode ==  KeyEvent.KEYCODE_HEADSETHOOK
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || keyCode == KeyEvent.KEYCODE_SPACE) {
            if (uniqueDown) {
                doPauseResume();
                extendTimeout();
                if (mPlayPauseButton != null) {
                    mPlayPauseButton.requestFocus();
                }
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
            if (uniqueDown && !mPlayer.isPlaying()) {
                mPlayer.start();
                updatePausePlayButtonState();
                extendTimeout();
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            if (uniqueDown && mPlayer.isPlaying()) {
                mPlayer.pause();
                updatePausePlayButtonState();
                extendTimeout();
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

        extendTimeout();
        return super.dispatchKeyEvent(event);
    }

    private View.OnClickListener mPlayPauseListener = new View.OnClickListener() {
        public void onClick(View v) {
            doPauseResume();
            extendTimeout();
        }
    };

    private View.OnClickListener mFullscreenListener = new View.OnClickListener() {
        public void onClick(View v) {
            doToggleFullscreen();
            extendTimeout();
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
        }
        else {
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
            show(0);

            mDragging = true;

            // By removing these pending progress messages we make sure
            // that a) we won't update the progress while the user adjusts
            // the seekbar and b) once the user is done dragging the thumb
            // we will post one of these messages to the queue again and
            // this ensures that there will be exactly one message queued up.
            mHandler.removeMessages(SHOW_PROGRESS);
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
            mPlayer.seekTo( (int) newposition);
            if (mCurrentTime != null)
                mCurrentTime.setText(stringForTime( (int) newposition));
        }

        public void onStopTrackingTouch(SeekBar bar) {
            mDragging = false;
            setProgress();
            updatePausePlayButtonState();
            show(sDefaultTimeout);

            // Ensure that progress is properly updated in the future,
            // the call to show() does not guarantee this because it is a
            // no-op if we are already showing.
            mHandler.sendEmptyMessage(SHOW_PROGRESS);
        }
    };

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (enabled) {
            syncButtonEnabledStates();
        }
    }

    private View.OnClickListener mRewListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mPlayer == null) {
                return;
            }

            int pos = mPlayer.getCurrentPosition();
            pos -= 5000; // milliseconds
            mPlayer.seekTo(pos);
            setProgress();

            extendTimeout();
        }
    };

    private View.OnClickListener mFfwdListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mPlayer == null) {
                return;
            }

            int pos = mPlayer.getCurrentPosition();
            pos += 15000; // milliseconds
            mPlayer.seekTo(pos);
            setProgress();

            extendTimeout();
        }
    };


    public void setPrevNextListeners(View.OnClickListener next, View.OnClickListener prev) {
        mNextListener = next;
        mPrevListener = prev;
        include(mNextButton, mNextListener != null, mNextListener);
        include(mPrevButton, mPrevListener != null, mPrevListener);
    }

    public interface MediaPlayerControl {
        void    start();
        void    pause();
        int     getDuration();
        int     getCurrentPosition();
        void    seekTo(int pos);
        boolean isPlaying();
        int     getBufferPercentage();
        boolean canPlay();
        boolean canSeekBackward();
        boolean canSeekForward();
        boolean canGoFullScreen();
        boolean isFullScreen();
        void    toggleFullScreen();
    }

    private static class MessageHandler extends Handler {
        private final WeakReference<MediaControllerView> mView;

        MessageHandler(MediaControllerView view) {
            mView = new WeakReference<MediaControllerView>(view);
        }
        @Override
        public void handleMessage(Message msg) {
            MediaControllerView view = mView.get();
            if (view == null || view.mPlayer == null) {
                return;
            }

            int pos;
            switch (msg.what) {
                case FADE_OUT:
                    view.hide();
                    break;
                case SHOW_PROGRESS:
                    pos = view.setProgress();
                    if (!view.mDragging && view.mShowing && view.mPlayer.isPlaying()) {
                        msg = obtainMessage(SHOW_PROGRESS);
                        sendMessageDelayed(msg, 1000 - (pos % 1000));
                    }
                    break;
            }
        }
    }
}
