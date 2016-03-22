package com.brentvatne.react;

import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;

import com.facebook.react.uimanager.ThemedReactContext;
import com.yqritc.scalablevideoview.ScalableType;
import com.yqritc.scalablevideoview.ScalableVideoView;

/**
 * Actual video surface view. Also ownas and encapsulates actual media player.
 */
public class ReactVideoView extends ScalableVideoView implements MediaPlayer.OnPreparedListener, MediaPlayer
        .OnErrorListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener {

    private ThemedReactContext mContext;
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

    private float mVolume = 1.0f;
    private float mRate = 1.0f;

    private boolean mMediaPlayerValid = false; // True if mMediaPlayer is in prepared, started, or paused state.
    private int mVideoDuration = 0;
    private int mVideoBufferedDuration = 0;

    private PlayerEventListener mListener;

    private static final int PROGRESS_UPDATE_INTERVAL = 250;

    public ReactVideoView(ThemedReactContext context, PlayerEventListener eventListener) {
        super(context);

        mContext = context;

        mListener = eventListener;

        initializeMediaPlayerIfNeeded();
        setSurfaceTextureListener(this);

        //TODO run only while playing
        mProgressUpdateRunnable = new Runnable() {
            @Override
            public void run() {

                if (mMediaPlayerValid) {
                    mListener.onProgress(mMediaPlayer.getCurrentPosition(), mVideoBufferedDuration);
                }
                mProgressUpdateHandler.postDelayed(mProgressUpdateRunnable, PROGRESS_UPDATE_INTERVAL);
            }
        };
        mProgressUpdateHandler.post(mProgressUpdateRunnable);

    }

    private void initializeMediaPlayerIfNeeded() {
        if (mMediaPlayer != null) {
            return;
        }
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
                setRawData(mContext.getResources().getIdentifier(
                        uriString,
                        "raw",
                        mContext.getPackageName()
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        mListener.onLoad(uriString, type, isNetwork);

        prepareAsync(this);
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

    @Override
    public void start() {
        super.start();
        mListener.onPlay();
    }

    @Override
    public void pause() {
        super.pause();
        mListener.onPause();
    }

    public void setVolume(float leftVolume, float rightVolume) {
        super.setVolume(leftVolume, rightVolume);
        mListener.onVolume(leftVolume, rightVolume);
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
        mListener.onMute(muted);
    }

    public void setVolumeModifier(final float volume) {
        mVolume = volume;
        setMutedModifier(mMuted);
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
//        setRateModifier(mRate);
    }




    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(ReactVideoViewManager.REACT_CLASS, "onPrepared() ");
        mMediaPlayerValid = true;
        mVideoDuration = mp.getDuration();

        mListener.onLoadComplete(mp.getCurrentPosition(), mVideoDuration);

        // This will play/pause based on initial setting for "paused"
        applyModifiers();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.d(ReactVideoViewManager.REACT_CLASS, "onError() " + what);
        mListener.onError(what, extra);
        return true;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        //Log.d(ReactVideoViewManager.REACT_CLASS, "onBufferingUpdate() " + percent);
        mVideoBufferedDuration = (int) Math.round((double) (mVideoDuration * percent) / 100.0);
        mListener.onBuffer(percent);
    }

    @Override
    public void seekTo(int msec) {
        if (mMediaPlayerValid) {
            super.seekTo(msec);
            mListener.onSeek(msec, getCurrentPosition());
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
        mListener.onStop();
    }

    public void doInit() {
        Log.d(ReactVideoViewManager.REACT_CLASS, "ReactVideoView.doInit() ");
        setSrc(mSrcUriString, mSrcType, mSrcIsNetwork, mSrcIsAsset);
    }

    public void doCleanup() {
        Log.d(ReactVideoViewManager.REACT_CLASS, "ReactVideoView.doCleanup() ");
        release();
        mMediaPlayerValid = false;
    }


    public  int getBufferPercentage() {
        return mVideoDuration > 0 ? mVideoBufferedDuration * 100 / mVideoDuration : mVideoDuration;
    }

}
