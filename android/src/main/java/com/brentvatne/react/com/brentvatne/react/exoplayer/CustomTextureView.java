package com.brentvatne.react.com.brentvatne.react.exoplayer;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;
import android.view.ViewGroup;

import com.brentvatne.react.ReactVideoViewManager;


public class CustomTextureView extends TextureView {

    private Listener listener;

    public CustomTextureView(Context context) {
        super(context);
    }

    public CustomTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public interface Listener {
        void onCustomTextureViewAttached();
    }

    public void setOnAttachListener(Listener listener) {
        this.listener = listener;
    }


    public CustomTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (listener != null) {
            listener.onCustomTextureViewAttached();
        }
    }

}
