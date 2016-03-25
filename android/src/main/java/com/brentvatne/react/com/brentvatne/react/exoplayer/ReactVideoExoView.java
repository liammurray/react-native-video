package com.brentvatne.react.com.brentvatne.react.exoplayer;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;

import com.brentvatne.RCTVideo.R;
import com.brentvatne.react.PlayerEventListener;
import com.brentvatne.react.ReactVideoViewManager;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.audio.AudioCapabilitiesReceiver;
import com.google.android.exoplayer.metadata.id3.Id3Frame;
import com.google.android.exoplayer.text.CaptionStyleCompat;
import com.google.android.exoplayer.text.Cue;
import com.google.android.exoplayer.text.SubtitleLayout;
import com.google.android.exoplayer.util.Util;
import com.xealth.mediacontroller.callback.Callback;
import com.xealth.mediacontroller.callback.WeakRefCallback;
import com.yqritc.scalablevideoview.ScalableType;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.List;


public class ReactVideoExoView extends FrameLayout
        implements ExoPlayerWrapper.Listener, ExoPlayerWrapper.CaptionListener, ExoPlayerWrapper.Id3MetadataListener,
        AudioCapabilitiesReceiver.Listener, SurfaceHolder.Callback {

    private Handler mHandler = new Handler();

    /** State that can be set prior to SRC */
    private ScalableType resizeMode = ScalableType.LEFT_TOP;
    private boolean enableRepeatMode = false;
    private boolean isPaused = false;
    private boolean isMuted = false;
    private float volume = 1.0f;
    private float playbackRate = 1.0f;
    private long playerPosition;

    private PlayerEventListener mListener;

    //TODO Set user agent
    private final String userAgent = Util.getUserAgent(getContext(), "ReactVideo");

    private static final CookieManager defaultCookieManager;
    static {
        defaultCookieManager = new CookieManager();
        defaultCookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    }

    /** Logs video events for debugging (from exoplayer demo) */
    private EventLogger eventLogger;

    private View debugRootView;
    private View shutterView;

    private SurfaceView surfaceView;

    private SubtitleLayout subtitleLayout;

    private ExoPlayerWrapper player;

    private boolean playerNeedsPrepare;


    private boolean enableBackgroundAudio;

    private Uri srcUri;
    private Uri contentUri;
    private int contentType;
    private String contentTypeExt; //"mp4", etc.
    private boolean conentIsNetwork;
    private boolean contentIsAsset;
    private String contentId;
    private String provider;

    private static boolean enableProgressCallbacks = true;

    private WeakRefCallback.DoRunnable progressRunnable = new WeakRefCallback.DoRunnable() {
        @Override
        public boolean doRun() {
            mListener.onProgress(player.getCurrentPosition());
            return enableProgressCallbacks && player.isPlaybackActive();
        }
    };
    private static final int PROGRESS_UPDATE_INTERVAL = 250;
    private Callback progressCallback = new WeakRefCallback(mHandler, PROGRESS_UPDATE_INTERVAL, progressRunnable);

    private AudioCapabilitiesReceiver audioCapabilitiesReceiver;

    public ReactVideoExoView(Context context) {
        super(context);
    }

    public ReactVideoExoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setEventListener(PlayerEventListener listener) {
        mListener = listener;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        // Use AspectRatioFrameLayout when no scale transform?
        // TODO get rid of the shutter?
        shutterView = findViewById(R.id.shutter);
        surfaceView = (SurfaceView) findViewById(R.id.surface_view);
        surfaceView.getHolder().addCallback(this);
        subtitleLayout = (SubtitleLayout) findViewById(R.id.subtitles);

        CookieHandler currentHandler = CookieHandler.getDefault();
        if (currentHandler != defaultCookieManager) {
            CookieHandler.setDefault(defaultCookieManager);
        }

        audioCapabilitiesReceiver = new AudioCapabilitiesReceiver(getContext(), this);


//        root.setOnKeyListener(new OnKeyListener() {
//            @Override
//            public boolean onKey(View v, int keyCode, KeyEvent event) {
//                if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE
//                        || keyCode == KeyEvent.KEYCODE_MENU) {
//                    return false;
//                }
//                return mediaController.dispatchKeyEvent(event);
//            }
//        });
    }


    private ExoPlayerWrapper.RendererBuilder getRendererBuilder() {
        switch (contentType) {
//            case Util.TYPE_SS:
//                return new SmoothStreamingRendererBuilder(this, userAgent, contentUri.toString(),
//                        new SmoothStreamingTestMediaDrmCallback());
//            case Util.TYPE_DASH:
//                return new DashRendererBuilder(this, userAgent, contentUri.toString(),
//                        new WidevineTestMediaDrmCallback(contentId, provider));
            case Util.TYPE_HLS:
                return new HlsRendererBuilder(getContext(), userAgent, contentUri.toString());
            case Util.TYPE_OTHER:
                return new ExtractorRendererBuilder(getContext(), userAgent, contentUri);
            default:
                throw new IllegalStateException("Unsupported type: " + contentType);
        }
    }

//    public ExoPlayerWrapper getPlayer() {
//        return player;
//    }

    static final String FILE_SCHEME="file";

    public void  setSrc(String uriString, final String type, final boolean isNetwork, final boolean isAsset) {
        srcUri = Uri.parse(uriString);
        Context context = getContext();

        if (isAsset) {
            if (!FILE_SCHEME.equals(srcUri.getScheme())) {
                contentUri = Uri.parse("asset://" + srcUri.getPath() + "." + type);
            } else {
                contentUri = srcUri;
            }
        } else if (!isNetwork) {
            // Raw resource TODO figure out why DefaultUriDataSourceWrapper fails (should work!)
            contentUri = Uri.parse("android.resource://" + context.getPackageName() + "/raw/" + uriString);
        } else {
            contentUri = srcUri;
        }

        Log.d(ReactVideoViewManager.REACT_CLASS, "ReactVideoView.setSrc(): original uri: " + srcUri);
        Log.d(ReactVideoViewManager.REACT_CLASS, "ReactVideoView.setSrc(): content uri:" + contentUri);

        contentTypeExt = type;
        conentIsNetwork = isNetwork;
        contentIsAsset = isAsset;
        // This type is used to determine mp4, hls, dash, etc. and therefore type of renderer
        contentType = MediaUtil.inferContentType(srcUri, type);
        contentId = "Unspecified contentId"; //TODO
        provider = "Unspecified provider"; //TODO
        configureSubtitleView();
        //if (requiresPermission(srcUri)) {} READ_EXTERNAL_STORAGE
        if (player == null) {
            preparePlayer(true);
        } else {
            player.setBackgrounded(false);
        }

    }



    /** Fetch URI metadata, and start playing if playWhenReady is true */
    private void preparePlayer(boolean playWhenReady) {
        if (player == null) {
            // Renderer handle obtaining data for a given URI
            player = new ExoPlayerWrapper(getRendererBuilder());
            player.addListener(this);
            player.setCaptionListener(this);
            player.setMetadataListener(this);
            applySavedState();
            playerNeedsPrepare = true;

            //mediaController.setMediaPlayer(player.getPlayerControl());
            //TODO mediaController.setEnabled(true);

            //TODO optional
            eventLogger = new EventLogger();
            eventLogger.startSession();
            player.addListener(eventLogger);
            player.setInfoListener(eventLogger);
            player.setInternalErrorListener(eventLogger);

        }
        if (playerNeedsPrepare) {
            // Attempt to build renderers, then play if setPlayWhenReady() was called
            player.prepare();
            playerNeedsPrepare = false;
            //updateButtonVisibilities();
        }
        player.setSurface(surfaceView.getHolder().getSurface());
        player.setPlayWhenReady(playWhenReady);

    }

    public void doInit() {
        Log.d(ReactVideoViewManager.REACT_CLASS, "ReactVideoView.doInit() ");
        audioCapabilitiesReceiver.register();
        //TODO no toString; do we need others?
        setSrc(srcUri.toString(), contentTypeExt, conentIsNetwork, contentIsAsset);
    }

    public void doCleanup() {
        Log.d(ReactVideoViewManager.REACT_CLASS, "ReactVideoView.doCleanup() ");
        audioCapabilitiesReceiver.unregister();
        releasePlayer();
    }

    private void releasePlayer() {
        progressCallback.cancel();
        if (player != null) {
            playerPosition = player.getCurrentPosition();
            player.stop();
            player.release();
            player = null;
            eventLogger.endSession();
            eventLogger = null;
        }
    }

    private void releaseOrBackgroundPlayer() {
        if (!enableBackgroundAudio) {
            releasePlayer();
        } else {
            player.setBackgrounded(true);
        }
        shutterView.setVisibility(View.VISIBLE);
    }

    private static boolean preparePending = false;

    // ExoPlayerWrapper.Listener implementation

    @Override
    public void onStateChanged(boolean playWhenReady, int playbackState) {
        if (playbackState == ExoPlayer.STATE_ENDED) {
            mListener.onStop();
        }

        switch(playbackState) {
            case ExoPlayer.STATE_BUFFERING:
                mListener.onBuffer(player.getBufferedPercentage(), player.getBufferedDuration());
                break;
            case ExoPlayer.STATE_ENDED:
                progressCallback.cancel();
                mListener.onStop();
                break;
            case ExoPlayer.STATE_IDLE:
                progressCallback.cancel();
                break;
            case ExoPlayer.STATE_PREPARING:
                progressCallback.cancel();
                preparePending = true;
                mListener.onLoad(srcUri.toString(), contentTypeExt, conentIsNetwork);
                break;
            case ExoPlayer.STATE_READY:
                if (preparePending) {
                    mListener.onLoadComplete(player.getCurrentPosition(), player.getDuration());
                    preparePending = false;
                }
                if (playWhenReady) {
                    mListener.onPlay();
                    progressCallback.set();
                } else {
                    mListener.onPause();
                    progressCallback.cancel();
                }
                break;
            default:
                //TODO
                break;
        }

    }


    @Override
    public void onCues(List<Cue> cues) {
        subtitleLayout.setCues(cues);
    }

    @Override
    public void onId3Metadata(List<Id3Frame> id3Frames) {
        Log.d(ReactVideoViewManager.REACT_CLASS, "onId3Metadata()");
    }

    @Override
    public void onError(Exception e) {
        //ExoPlaybackException, UnsupportedDrmException
        Log.d(ReactVideoViewManager.REACT_CLASS, "onError(): " + e);
        playerNeedsPrepare = true;
        mListener.onError(-101,-101); //TODO better codes
    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        shutterView.setVisibility(View.GONE);
        //TODO update matrix
        //scaleVideoSize(width, height);
        //videoFrame.setAspectRatio(height == 0 ? 1 : (width * pixelWidthAspectRatio) / height);
    }


    /** Pass something like ExoPlayerWrapper.TYPE_VIDEO to check how many tracks */
    public int getTrackCount(int type) {
        return player != null ? player.getTrackCount(type) : 0;
    }


    @Override
    public void onAudioCapabilitiesChanged(AudioCapabilities audioCapabilities) {
        if (player == null) {
            return;
        }
        boolean backgrounded = player.getBackgrounded();
        boolean playWhenReady = player.getPlayWhenReady();
        releasePlayer();
        preparePlayer(playWhenReady);
        player.setBackgrounded(backgrounded);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (player != null) {
            player.setSurface(holder.getSurface());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        //TODO update matrix
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (player != null) {
            player.blockingClearSurface();
        }
    }

    private void configureSubtitleView() {
        CaptionStyleCompat style;
        float fontScale;
        if (Util.SDK_INT >= 19) {
            style = MediaUtil.getUserCaptionStyleV19(getContext());
            fontScale = MediaUtil.getUserCaptionFontScaleV19(getContext());
        } else {
            style = CaptionStyleCompat.DEFAULT;
            fontScale = 1.0f;
        }
        subtitleLayout.setStyle(style);
        subtitleLayout.setFractionalTextSize(SubtitleLayout.DEFAULT_TEXT_SIZE_FRACTION * fontScale);
    }




    public void setResizeMode(final ScalableType resizeMode) {
        this.resizeMode = resizeMode; //TODO
        //setScalableType(resizeMode);
        //invalidate();
    }


    public void setRepeat(final boolean repeat) {
        enableRepeatMode = repeat;
        //TODO
    }

    public void setPaused(final boolean paused) {
        isPaused = paused;
        if (player == null || !player.canPlay()) {
            return;
        }
        player.setPlayWhenReady(!isPaused);
    }

    public void setMuted(final boolean muted) {
        isMuted = muted;
        if (player == null || !player.canPlay()) {
            return;
        }
        //TODO
        mListener.onMute(muted);
    }


    public long getDuration() {
        return (player != null) ? player.getDuration() : 0;
    }

    public long getCurrentPosition() {
        return (player != null) ? player.getCurrentPosition() : 0;
    }

    public int getBufferedPercentage() {
        return (player != null) ? player.getBufferedPercentage() : 0;
    }
    public long getBufferedDuration() {
        return (player != null) ? player.getBufferedDuration() : 0;
    }

    public boolean canPlay() {
        return (player != null) && player.canPlay();
    }

    public boolean isPlaying() {
        return (player != null) && player.isPlaybackActive();
    }

    @Override
    public void onSeek(long oldPos, long newPos) {
        mListener.onSeek(newPos, oldPos);
    }

    public void seekTo(long pos) {

        playerPosition = pos;
        if (player == null || !player.canPlay()) {
            return;
        }
        playerPosition = player.getDuration() == ExoPlayer.UNKNOWN_TIME ? 0
                : Math.min(Math.max(0, pos), player.getDuration());
        //long oldPos = player.getCurrentPosition();
        player.seekTo(playerPosition);
        //mListener.onSeek(playerPosition, player.getCurrentPosition());
    }

    public void setVolume(final float volume) {
        this.volume = volume;
        setMuted(isMuted);
    }

    public void setRateModifier(final float rate) {
        playbackRate = rate; //TODO
    }

    /**
     * State that may have been set before player created
     */
    private void applySavedState() {
        player.seekTo(playerPosition);
        setResizeMode(resizeMode);
        setRepeat(enableRepeatMode);
        setPaused(isPaused);
        setMuted(isMuted);
        setRateModifier(playbackRate);
        seekTo(playerPosition);
    }

}
