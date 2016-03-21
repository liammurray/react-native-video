package com.brentvatne.react;

import android.graphics.Color;
import android.widget.FrameLayout;

import com.facebook.react.uimanager.ThemedReactContext;

/**
 * Frame that parents video view and media transport controller (player controls)
 */
public class ReactVideoViewContainer extends FrameLayout {

    private ReactVideoView mVideoView;

    public ReactVideoViewContainer(ThemedReactContext themedReactContext, ReactVideoHostView hostView) {
        super(themedReactContext);
        mVideoView = new ReactVideoView(themedReactContext, hostView);
        addView(mVideoView, newFrameLayoutParamsForEmbed());
        setBackgroundColor(Color.BLACK);
    }

    private FrameLayout.LayoutParams newFrameLayoutParamsForEmbed() {
        return new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
    }

    public ReactVideoView getVideoView() {
        return mVideoView;
    }

}




