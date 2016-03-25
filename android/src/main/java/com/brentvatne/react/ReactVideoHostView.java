package com.brentvatne.react;

import android.annotation.TargetApi;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.brentvatne.react.com.brentvatne.react.exoplayer.ReactVideoExoView;
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

    public void setBackground(Drawable background) {
        super.setBackground(background);
        // Set background in view container so it inherits when re-attached in fullscreen (TODO probably need better solution)
        mVideoViewContainer.setBackground(background);
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


    /** Expand to fill screen */
    private LayoutParams newFrameLayoutParamsForFullScreen() {
        return new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);
    }

    /** Wrap around content */
    private LayoutParams newFrameLayoutParamsForEmbed() {
        return new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public boolean canGoFullScreen() {
        return mOverlayView != null && mOverlayView.isAttachedToWindow();
    }

    private static void reParentView(ViewGroup parent, View child, LayoutParams params) {
        child.onStartTemporaryDetach(); //TODO Necessary? Useful?
        ViewUtil.detachFromParent(child);
        child.destroyDrawingCache(); // Not sure necessary
        parent.addView(child, params);
        child.onFinishTemporaryDetach();
    }

    public boolean goFullScreen() {
        if (!mIsFullScreen && canGoFullScreen()) {
            reParentView(mOverlayView, mVideoViewContainer, newFrameLayoutParamsForFullScreen());
            mIsFullScreen = true;
            mVideoViewContainer.onFullScreenSwitch();
            return true;
        }
        return false;
    }

    public boolean goEmbed() {
        if (mIsFullScreen) {
            Log.d("RCTVideo", "ReactVideoHostView: goEmbed(): win token: " + getWindowToken());
            reParentView(this, mVideoViewContainer, newFrameLayoutParamsForEmbed());
            mIsFullScreen = false;
            mVideoViewContainer.onFullScreenSwitch();
            return true;
        }
        return false;
    }

    public ReactVideoViewContainer getContainerView() {
        return mVideoViewContainer;
    }


    public ReactVideoExoView getVideoView() {
        return mVideoViewContainer.getVideoView();
    }

    public boolean isFullScreen() {
        return mIsFullScreen;
    }

}




