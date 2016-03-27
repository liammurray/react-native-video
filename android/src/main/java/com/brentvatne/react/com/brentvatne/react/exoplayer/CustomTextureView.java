package com.brentvatne.react.com.brentvatne.react.exoplayer;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;
import android.view.ViewGroup;

import com.brentvatne.react.ReactVideoViewManager;


public class CustomTextureView extends TextureView {
    private SurfaceTexture persistTexture;

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

    public SurfaceTexture getPersistTexture() {
        return persistTexture;
    }
    public boolean setPersistTexture(SurfaceTexture texture) {
        if (persistTexture != texture) {
            persistTexture = texture;
            return true;
        }
        return false;
    }

    public boolean releasePersistTexture() {
        if (persistTexture != null) {
            persistTexture.release();
            persistTexture = null;
            return true;
        }
        return false;
    }

    public CustomTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Log.d(ReactVideoViewManager.REACT_CLASS, "CustonTextureView:onAttachedToWindow(): st: " + persistTexture);
        if (persistTexture != null) {
            setSurfaceTexture(persistTexture);
        }
        if (listener != null) {
            listener.onCustomTextureViewAttached();
        }
    }

}
