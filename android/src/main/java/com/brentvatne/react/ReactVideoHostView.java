package com.brentvatne.react;

import android.annotation.TargetApi;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.brentvatne.react.com.brentvatne.react.exoplayer.ReactVideoExoView;
import com.facebook.react.uimanager.ThemedReactContext;
import com.xealth.mediacontroller.callback.Callback;
import com.xealth.mediacontroller.callback.WeakRefCallback;

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

        // Typically Javascript will know expected aspect ration in advance and size this host view
        // We therefore want embedded container to size to this host view.
        mVideoViewContainer = new ReactVideoViewContainer(themedReactContext, this);
        addView(mVideoViewContainer, newMatchParentFrameLayoutParams());
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

    private LayoutParams newMatchParentFrameLayoutParams() {
        return new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);
    }


    @TargetApi(Build.VERSION_CODES.KITKAT)
    public boolean canGoFullScreen() {
        return mOverlayView != null && mOverlayView.isAttachedToWindow();
    }

    private static void reParentView(ViewGroup parent, View child, LayoutParams params) {
        child.onStartTemporaryDetach();
        ViewUtil.detachFromParent(child);
        parent.addView(child, params);
        child.onFinishTemporaryDetach();
    }


    private WeakRefCallback.DoRunnable layoutRunnable = new WeakRefCallback.DoRunnable() {
        @Override
        public boolean doRun() {
            //Log.d("RCTVideo", "ReactVideoHostView: doRun(): measure and layout");
            measure(
                    MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.EXACTLY));
            layout(getLeft(), getTop(), getRight(), getBottom());
            return false;
        }
    };
    private final Callback layoutCallback = new WeakRefCallback(new Handler(), 0, layoutRunnable);


    @Override
    public void requestLayout() {
        super.requestLayout();
        // When embedded the container resides within a ReactViewGroup.
        // ReactViewGroup ignores/shorts-circuits requestLayout!
        if (!mIsFullScreen && layoutCallback != null) {
            layoutCallback.set();
        }
    }


    public boolean goFullScreen() {
        if (!mIsFullScreen && canGoFullScreen()) {
            mIsFullScreen = true;
            reParentView(mOverlayView, mVideoViewContainer, newMatchParentFrameLayoutParams());
            mVideoViewContainer.onFullScreenSwitch();
            return true;
        }
        return false;
    }

    public boolean goEmbed() {
        if (mIsFullScreen) {
            mIsFullScreen = false;
            reParentView(this, mVideoViewContainer, newMatchParentFrameLayoutParams());
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

//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        Log.d(ReactVideoViewManager.REACT_CLASS, "ReactVideoHostView.onMeasure(): " + ViewUtil.describeMeasureInfo(this, getSuggestedMinimumWidth(), getSuggestedMinimumHeight(), widthMeasureSpec, heightMeasureSpec));
//    }
//
//    @Override
//    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
//        super.onLayout(changed, left, top, right, bottom);
//        Log.d(ReactVideoViewManager.REACT_CLASS, "ReactVideoHostView.onLayout(): " + ViewUtil.describeSize(left, top, right, bottom));
//    }

}




