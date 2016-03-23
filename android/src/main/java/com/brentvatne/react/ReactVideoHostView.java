package com.brentvatne.react;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import com.facebook.react.uimanager.ThemedReactContext;

/**
 * The top-level view bound to RCTVideo. It hosts the video view only while embedded.
 * but otherwise persists in the layout (list view, etc.). The container view that contains
 * the video view will be re-parented depending on fullscreen playback state.
 * The container will be added to the host during embedded state and to the overlay
 * during fullscreen state.
 */
public class ReactVideoHostView extends FrameLayout {

    private ReactVideoViewContainer mVideoViewContainer;

    private OverlayView mOverlayView;

    private boolean mIsFullScreen = false;

    private static final boolean enableAutoOverlay = true;

    public ReactVideoHostView(ThemedReactContext themedReactContext, OverlayView overlayView) {
        super(themedReactContext);
        mOverlayView = overlayView;

        mVideoViewContainer = new ReactVideoViewContainer(themedReactContext, this);
        addView(mVideoViewContainer, newFrameLayoutParamsForEmbed());
    }


    public ReactVideoHostView(ThemedReactContext themedReactContext) {
        this(themedReactContext, null);
    }

    private void ensureOverlayView() {
        if (mOverlayView == null) {
            mOverlayView = OverlayView.getOverlay(this);
            if (mOverlayView == null) {
                Log.d(ReactVideoViewManager.REACT_CLASS, "ReactVideoHostView.ensureOverlayView() creating overlay");
                mOverlayView = new OverlayView(getContext(), null);
                mOverlayView.attach(this);
            } else {
                Log.d(ReactVideoViewManager.REACT_CLASS, "ReactVideoHostView.ensureOverlayView() found overlay");
            }
        }
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Log.d(ReactVideoViewManager.REACT_CLASS, "ReactVideoHostView.onAttachedToWindow() ");
        if (enableAutoOverlay) {
            ensureOverlayView();
        }
        mVideoViewContainer.doInit();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.d(ReactVideoViewManager.REACT_CLASS, "ReactVideoHostView.onDetachedFromWindow() ");
        if (mIsFullScreen) {
            // May have issues doing it here since ViewGroup may be iterating over heirarchy
            goEmbed();
        }
        mVideoViewContainer.doCleanup();
    }



    private LayoutParams newFrameLayoutParamsForEmbed() {
        return new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public boolean canGoFullScreen() {
        return mOverlayView != null && mOverlayView.isAttachedToWindow();
        //return mOverlayView != null && mOverlayView.getWindowToken() != null;
    }

    public boolean goFullScreen() {
        if (!mIsFullScreen && canGoFullScreen()) {
            //int cx = view.getMeasuredWidth();
            //int cy = view.getMeasuredHeight();
            ViewUtil.detachFromParent(mVideoViewContainer);
            // Default FrameLayout params specify MATCH_PARENT
            mOverlayView.addView(mVideoViewContainer);
            mVideoViewContainer.onPostFullScreenToggle(true);
            mIsFullScreen = true;
            return true;
        }
        return false;
    }

    public boolean goEmbed() {
        if (mIsFullScreen) {
            Log.d("RCTVideo", "ReactVideoHostView: goEmbed(): this win token: " + getWindowToken());
            ViewUtil.detachFromParent(mVideoViewContainer);
            addView(mVideoViewContainer, newFrameLayoutParamsForEmbed());
            mVideoViewContainer.setVisibility(View.INVISIBLE);
            mVideoViewContainer.setVisibility(View.VISIBLE);
            mVideoViewContainer.onPostFullScreenToggle(false);
            mIsFullScreen = false;
            return true;
        }
        return false;
    }

    public ReactVideoViewContainer getContainerView() {
        return mVideoViewContainer;
    }

    public ReactVideoView getVideoView() {
        return mVideoViewContainer.getVideoView();
    }

    public boolean isFullScreen() {
        return mIsFullScreen;
    }

}




