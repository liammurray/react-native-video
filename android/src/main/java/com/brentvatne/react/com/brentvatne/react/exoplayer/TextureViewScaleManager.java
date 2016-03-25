package com.brentvatne.react.com.brentvatne.react.exoplayer;

import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import com.brentvatne.react.ReactVideoViewManager;
import com.yqritc.scalablevideoview.ScalableType;
import com.yqritc.scalablevideoview.ScaleManager;
import com.yqritc.scalablevideoview.Size;


public class TextureViewScaleManager implements TextureView.SurfaceTextureListener {

    private ScalableType mScalableType = ScalableType.NONE;
    
    private int sourceWidth;
    
    private int sourceHeight;

    private TextureView textureView;

    private SurfaceUser surfaceUser;


    public interface SurfaceUser {
        void setSurface(Surface surface);
    }

    public TextureViewScaleManager(TextureView textureView, SurfaceUser user) {
        this.textureView = textureView;
        this.surfaceUser = user;
        textureView.setSurfaceTextureListener(this);
    }
    
    public void setSourceSize(int width, int height) {
        sourceWidth = width;
        sourceHeight = height;
        updateMatrix(width, height);
    }

    public void setScalableType(ScalableType scalableType) {
        mScalableType = scalableType;
        updateMatrix(sourceWidth, sourceHeight);
    }

    private Surface surface;

    public Surface getSurface() {
        return surface;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        surface = new Surface(surfaceTexture);
        if (surfaceUser != null) {
            surfaceUser.setSurface(surface);
            updateMatrix(sourceWidth, sourceHeight, width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        updateMatrix(sourceWidth, sourceHeight);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        this.surface = null;
        if (surfaceUser != null) {
            surfaceUser.setSurface(null);
        }
        // Always return true so texture is released
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        //Log.d(ReactVideoViewManager.REACT_CLASS, "onSurfaceTextureUpdated()");
    }
    

    private void updateMatrix(int sourceWidth, int sourceHeight) {
        if (sourceWidth == 0 || sourceHeight == 0) {
            return;
        }
        updateMatrix(sourceWidth, sourceHeight, textureView.getWidth(), textureView.getHeight());
    }

    private void updateMatrix(int sourceWidth, int sourceHeight, int surfaceWidth, int surfaceHeight) {
        if (sourceWidth == 0 || sourceHeight == 0) {
            return;
        }

        Size viewSize = new Size(surfaceWidth, surfaceHeight);
        Size videoSize = new Size(sourceWidth, sourceHeight);
        ScaleManager scaleManager = new ScaleManager(viewSize, videoSize);
        Matrix matrix = scaleManager.getScaleMatrix(mScalableType);
        textureView.setTransform(matrix);
        textureView.invalidate();
    }


    
}