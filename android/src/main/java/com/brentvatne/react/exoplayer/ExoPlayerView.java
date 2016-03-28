package com.brentvatne.react.exoplayer;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.widget.FrameLayout;

import com.brentvatne.RCTVideo.BuildConfig;
import com.brentvatne.RCTVideo.R;
import com.brentvatne.react.PlayerEventListener;
import com.brentvatne.react.ReactVideoModelState;
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

/**
 * View that renders ExoPlayer video
 */
public class ExoPlayerView extends FrameLayout
        implements ExoPlayerWrapper.Listener, ExoPlayerWrapper.CaptionListener, ExoPlayerWrapper.Id3MetadataListener,
        AudioCapabilitiesReceiver.Listener/*, SurfaceHolder.Callback*/, TextureViewHelper.SurfaceUser {

    private static final String LOGTAG = ExoPlayerWrapper.class.getSimpleName();
    private Handler mHandler = new Handler();


    private long playerPosition;

    private PlayerEventListener mListener;

    private static final String FILE_SCHEME = "file";

    //TODO Set user agent
    private final String userAgent = Util.getUserAgent(getContext(), "ReactVideo");

    private static final CookieManager defaultCookieManager;
    static {
        defaultCookieManager = new CookieManager();
        defaultCookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    }

    /** Logs video events for debugging (extremely useful, from exoplayer demo) */
    private EventLogger eventLogger;

    private static final boolean eventLoggerEnabled = BuildConfig.DEBUG;

    private TextureView textureView;
    private TextureViewHelper textureViewHelper;

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
    private static boolean enableBufferProgressCallbacks = true;

    private WeakRefCallback.DoRunnable progressRunnable = new WeakRefCallback.DoRunnable() {
        @Override
        public boolean doRun() {
            mListener.onProgress(player.getCurrentPosition());
            return enableProgressCallbacks && player.isPlaying();
        }
    };
    private static final int PROGRESS_UPDATE_INTERVAL = 250;
    private Callback progressCallback = new WeakRefCallback(mHandler, PROGRESS_UPDATE_INTERVAL, progressRunnable);

    private WeakRefCallback.DoRunnable bufferingRunnable = new WeakRefCallback.DoRunnable() {
        @Override
        public boolean doRun() {
            mListener.onBuffer(player.getBufferedPercentage(), player.getBufferedDuration());
            return enableBufferProgressCallbacks && player.isBuffering();
        }
    };
    private static final int BUFFER_UPDATE_INTERVAL = 500;
    private Callback bufferingCallback = new WeakRefCallback(mHandler, BUFFER_UPDATE_INTERVAL, bufferingRunnable);


    private AudioCapabilitiesReceiver audioCapabilitiesReceiver;

    public ExoPlayerView(Context context) {
        super(context);
    }

    public ExoPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setEventListener(PlayerEventListener listener) {
        mListener = listener;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        textureView = (TextureView) findViewById(R.id.texture_view);
        textureViewHelper = new TextureViewHelper(textureView, this);
        subtitleLayout = (SubtitleLayout) findViewById(R.id.subtitles);
        CookieHandler currentHandler = CookieHandler.getDefault();
        if (currentHandler != defaultCookieManager) {
            CookieHandler.setDefault(defaultCookieManager);
        }
        audioCapabilitiesReceiver = new AudioCapabilitiesReceiver(getContext(), this);
    }


    private ExoPlayerWrapper.RendererBuilder getRendererBuilder() {
        Log.d(LOGTAG, "getRendererBuilder(): creating builder for: " + contentUri);
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

    private ReactVideoModelState model;

    public void prepareVideo(ReactVideoModelState model) {
        this.model = model;
        textureViewHelper.setScalableType(model.getResizeMode());
        prepareVideo(model.getContentUri(), model.getContentType(), model.isContentNetwork(), model.isContentAsset(), !model.isPaused());
    }

    public void prepareVideo(String uriString,
                             String type,
                             boolean isNetwork,
                             boolean isAsset,
                             boolean playWhenReady) {
        releasePlayer();

        srcUri = Uri.parse(uriString);
        Context context = getContext();

        if (isNetwork) {
            contentUri = srcUri;
        } else if (isAsset) {
            if (!FILE_SCHEME.equals(srcUri.getScheme())) {
                contentUri = Uri.parse("asset://" + srcUri.getPath() + "." + type);
            } else {
                contentUri = srcUri;
            }
        } else {
            // Raw resource
            contentUri = Uri.parse("android.resource://" + context.getPackageName() + "/raw/" + uriString);
        }

        Log.d(LOGTAG, "prepareVideo(): content uri: " + contentUri);

        contentTypeExt = type;
        conentIsNetwork = isNetwork;
        contentIsAsset = isAsset;
        // This type is used to determine mp4, hls, dash, etc. and therefore type of renderer
        contentType = ExoPlayerUtil.inferContentType(srcUri, type);
        contentId = "Unspecified contentId"; //TODO
        provider = "Unspecified provider"; //TODO
        configureSubtitleView();
        if (isForeground) {
            preparePlayer(playWhenReady);
        }
    }

    private void createPlayer() {
        Log.d(LOGTAG, "createPlayer()");
        // Renderer handle obtaining data for a given URI
        player = new ExoPlayerWrapper(getRendererBuilder());
        player.setSurface(textureViewHelper.getSurface());
        player.addListener(this);
        player.setCaptionListener(this);
        player.setMetadataListener(this);
        // Init default values
        if (model != null) {
            setResizeMode(model.getResizeMode());
            setRepeat(model.isEnableRepeatMode());
            setPaused(model.isPaused());
            setMuted(model.isMuted());
            setVolume(model.getVolume());
            setPlaybackRate(model.getPlaybackRate());
            seekTo(model.getPos());
        }
        playerNeedsPrepare = true;
        if (eventLoggerEnabled) {
            eventLogger = new EventLogger();
            eventLogger.startSession();
            player.addListener(eventLogger);
            player.setInfoListener(eventLogger);
            player.setInternalErrorListener(eventLogger);
        }
    }
    /** Fetch URI metadata, and start playing if playWhenReady is true */
    private void preparePlayer(boolean playWhenReady) {
        if (player == null) {
            createPlayer();
        } else {
            player.setSurface(textureViewHelper.getSurface());
        }
        if (playerNeedsPrepare) {
            // Attempt to build renderers. Auto plays bases on how setPlayWhenReady() was called...
            player.prepare();
            playerNeedsPrepare = false;
        }
        player.setPlayWhenReady(playWhenReady);
    }

    private void releasePlayer() {
        progressCallback.cancel();
        bufferingCallback.cancel();
        if (player != null) {
            Log.d(LOGTAG, "releasePlayer(): destroy");
            playerPosition = player.getCurrentPosition();
            player.stop();
            player.release();
            player = null;
            if (eventLogger != null) {
                eventLogger.endSession();
                eventLogger = null;
            }
        }
    }


    private boolean isForeground = false;

    /**
     * Activity resuming, Hosting view attaches to window, etc.
     */
    public void init() {
        Log.d(LOGTAG, "init() ");
        isForeground = true;
        audioCapabilitiesReceiver.register();
        textureViewHelper.enablePersistTexture(true);
        if (player == null) {
            preparePlayer(model != null ? !model.isPaused() : false);
        } else {
            // Player audio continued. Resume video.
            player.setBackgrounded(false);
        }
    }

    /**
     * Activity pausing or going away, Hosting view detaching from window, etc.
     */
    public void cleanUp(boolean fullCleanup) {
        Log.d(LOGTAG, "cleanUp(): full: " + fullCleanup);
        isForeground = false;
        audioCapabilitiesReceiver.unregister();
        textureViewHelper.enablePersistTexture(false);
        if (fullCleanup || !enableBackgroundAudio) {
            releasePlayer();
        } else {
            // Pause video but continue audio
            player.setBackgrounded(true);
        }
    }

    private static boolean preparePending = false;

    // ExoPlayerWrapper.Listener implementation

    @Override
    public void onStateChanged(boolean playWhenReady, int prevPlaybackState, int playbackState) {

        if (playbackState != ExoPlayer.STATE_READY) {
            progressCallback.cancel();
        }
        if (playbackState != ExoPlayer.STATE_BUFFERING) {
            bufferingCallback.cancel();
        }
        boolean isStateChange = (prevPlaybackState != playbackState);
        if (isStateChange) {
            switch (playbackState) {
                case ExoPlayer.STATE_BUFFERING:
                    // ExoPlayer only notifies once when we transition to buffering state
                    mListener.onBuffer(player.getBufferedPercentage(), player.getBufferedDuration());
                    bufferingCallback.set();
                    break;
                case ExoPlayer.STATE_ENDED:
                case ExoPlayer.STATE_IDLE: // Idle may occur if we never make it to READY: I->P->B->(decode error)->I
                    // Always play from beginning even though internally player may stop and maintain pos at end
                    playerPosition = 0;
                    mListener.onStop();
                    break;
                case ExoPlayer.STATE_PREPARING:
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
        } else {
            switch (playbackState) {
                case ExoPlayer.STATE_READY:
                    if (playWhenReady) {
                        mListener.onPlay();
                        progressCallback.set();
                    } else {
                        mListener.onPause();
                        progressCallback.cancel();
                    }
                    break;
                default:
                    break;
            }
        }

    }


    @Override
    public void onCues(List<Cue> cues) {
        subtitleLayout.setCues(cues);
    }

    @Override
    public void onId3Metadata(List<Id3Frame> id3Frames) {
    }

    @Override
    public void onError(Exception e) {
        //ExoPlaybackException, UnsupportedDrmException
        Log.d(LOGTAG, "onError(): " + e);
        playerNeedsPrepare = true;
        mListener.onError(e, true);
    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        Log.d(LOGTAG, "onVideoSizeChanged(): " + width + "," + height);
        textureViewHelper.setSourceSize(width, height);
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
    public void setSurface(Surface surface) {
        //Log.d(LOGTAG, "setSurface(): surface: " + surface);
        if (player != null) {
            if (surface != null) {
                player.setSurface(surface);
            } else {
                player.setSurface(null, true);
            }
        }
    }


    private void configureSubtitleView() {
        CaptionStyleCompat style;
        float fontScale;
        if (Util.SDK_INT >= 19) {
            style = ExoPlayerUtil.getUserCaptionStyleV19(getContext());
            fontScale = ExoPlayerUtil.getUserCaptionFontScaleV19(getContext());
        } else {
            style = CaptionStyleCompat.DEFAULT;
            fontScale = 1.0f;
        }
        subtitleLayout.setStyle(style);
        subtitleLayout.setFractionalTextSize(SubtitleLayout.DEFAULT_TEXT_SIZE_FRACTION * fontScale);
    }




    public void setResizeMode(final ScalableType resizeMode) {
        textureViewHelper.setScalableType(resizeMode);
    }


    public void setRepeat(final boolean repeat) {
        if (player == null || !player.canPlay()) {
            return;
        }
        player.setRepeatMode(repeat);
    }


    public void setPaused(final boolean paused) {
        if (player == null || !player.canPlay()) {
            return;
        }
        player.setPlayWhenReady(!paused);
        if (player.isIdle()) {
            player.prepare();
        }
    }

    public void setMuted(final boolean muted) {
        if (player != null) {
            player.setMute(muted);
            mListener.onMute(muted);
        }

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
        return (player != null) && player.isPlaying();
    }

    @Override
    public void onSeek(long oldPos, long newPos) {
        mListener.onSeek(newPos, oldPos);
    }

    public void seekTo(long pos) {
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
        if (player != null) {
            player.setVolume(volume);
            mListener.onVolume(volume, volume);
        }
    }

    public void setPlaybackRate(final float rate) {
        Log.e(LOGTAG, "setPlaybackRate(): not implemented");
    }




}
