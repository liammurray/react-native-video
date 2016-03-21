package com.brentvatne.react;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;
import android.widget.FrameLayout;

import com.facebook.react.uimanager.ThemedReactContext;

/**
 * The top-level view bound to RCTVideo. It hosts the video view while embedded.
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
        mVideoViewContainer.getVideoView().doInit();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.d(ReactVideoViewManager.REACT_CLASS, "ReactVideoHostView.onDetachedFromWindow() ");
        if (mIsFullScreen) {
            // May have issues doing it here since ViewGroup may be iterating over heirarchy
            goEmbed();
        }
        mVideoViewContainer.getVideoView().doCleanup();
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
            mIsFullScreen = true;
            return true;
        }
        return false;
    }

    public boolean goEmbed() {
        if (mIsFullScreen) {
            ViewUtil.detachFromParent(mVideoViewContainer);
            addView(mVideoViewContainer, newFrameLayoutParamsForEmbed());
            mIsFullScreen = false;
            return true;
        }
        return false;
    }
    public ReactVideoView getVideoView() {
        return mVideoViewContainer.getVideoView();
    }

    public boolean isFullScreen() {
        return mIsFullScreen;
    }
}




