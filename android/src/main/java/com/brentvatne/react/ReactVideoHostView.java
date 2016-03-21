package com.brentvatne.react;

import android.util.Log;
import android.widget.FrameLayout;

import com.facebook.react.uimanager.ThemedReactContext;

/**
 * The top-level view bound to RCTVideo. It hosts the video view while embedded.
 */
public class ReactVideoHostView extends FrameLayout {

    private ThemedReactContext mThemedReactContext;

    private ReactVideoViewContainer mVideoViewContainer;

    private OverlayView mOverlayView;

    private boolean mIsFullScreen = false;

    private boolean mFirstPending = true;

    public ReactVideoHostView(ThemedReactContext themedReactContext, OverlayView overlayView) {
        super(themedReactContext);
        mOverlayView = overlayView;

        mThemedReactContext = themedReactContext;
        mVideoViewContainer = new ReactVideoViewContainer(themedReactContext, this);
        addView(mVideoViewContainer, newFrameLayoutParamsForEmbed());
    }


    public ReactVideoHostView(ThemedReactContext themedReactContext) {
        this(themedReactContext, null);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Log.d(ReactVideoViewManager.REACT_CLASS, "ReactVideoHostView.onAttachedToWindow() ");
        if (mFirstPending) {
            mVideoViewContainer.getVideoView().doInit();
            mFirstPending = false;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.d(ReactVideoViewManager.REACT_CLASS, "ReactVideoHostView.onDetachedFromWindow() ");
        if (mIsFullScreen) {
            ViewUtil.detachFromParent(mVideoViewContainer);
        }
        mVideoViewContainer.getVideoView().doCleanup();
    }



    private LayoutParams newFrameLayoutParamsForEmbed() {
        return new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
    }

    public boolean canGoFullScreen() {
        return mOverlayView != null && mOverlayView.getWindowToken() != null;
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




