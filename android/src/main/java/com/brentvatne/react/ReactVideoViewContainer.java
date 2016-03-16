package com.brentvatne.react;

import android.widget.FrameLayout;

import com.facebook.react.uimanager.ThemedReactContext;

public class ReactVideoViewContainer extends FrameLayout {

    private ThemedReactContext mThemedReactContext;

    private ReactVideoView mVideoView;

    public ReactVideoViewContainer(ThemedReactContext themedReactContext) {
        super(themedReactContext);
        mThemedReactContext = themedReactContext;
        mVideoView = new ReactVideoView(themedReactContext);
        addView(mVideoView, newFrameLayoutParams());
    }

    private FrameLayout.LayoutParams newFrameLayoutParams() {
        return new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
    }

    public ReactVideoView getVideoView() {
        return mVideoView;
    }
}




