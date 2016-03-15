package com.brentvatne.react;


import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.MediaController;

public class ReactVideoMediaController extends MediaController {
    public ReactVideoMediaController(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
    }

    public ReactVideoMediaController(Context context, boolean useFastForward) {
        super(context, useFastForward);
    }

    public ReactVideoMediaController(Context context) {
        super(context);
    }

    public interface VisibilityListener {
        public void onControllerVisibilityChanged(boolean attached);
    }

    private VisibilityListener visibilityListener;

    public void setVisibilityListener(VisibilityListener listener) {
        visibilityListener = listener;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (visibilityListener != null) {
            visibilityListener.onControllerVisibilityChanged(true);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (visibilityListener != null) {
            visibilityListener.onControllerVisibilityChanged(false);
        }
    }

}
