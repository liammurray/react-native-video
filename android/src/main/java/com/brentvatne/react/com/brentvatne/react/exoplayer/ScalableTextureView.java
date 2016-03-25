package com.brentvatne.react.com.brentvatne.react.exoplayer;

import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.view.TextureView;

import com.yqritc.scalablevideoview.ScalableType;
import com.yqritc.scalablevideoview.ScaleManager;
import com.yqritc.scalablevideoview.Size;


public class ScalableTextureView implements TextureView.SurfaceTextureListener {

    private ScalableType mScalableType = ScalableType.NONE;
    
    private int sourceWidth;
    
    private int sourceHeight;

    private TextureView textureView;

    private SurfaceUser surfaceUser;


    public interface SurfaceUser {
        void setSurface(Surface surface);
    }

    public ScalableTextureView(TextureView textureView, SurfaceUser user) {
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

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        Surface surface = new Surface(surfaceTexture);
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
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (surfaceUser != null) {
            surfaceUser.setSurface(null);
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
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
        if (matrix != null) {
            textureView.setTransform(matrix);
            textureView.invalidate(); //TODO necessary?
        }
    }


    
}