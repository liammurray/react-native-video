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

    private CustomTextureView customTextureView;

    private SurfaceUser surfaceUser;

    private boolean isPersisting = true;

    private Surface surface;

    public Surface getSurface() {
        return surface;
    }

    public interface SurfaceUser {
        void setSurface(Surface surface);
    }

    private void setPersistTexture(SurfaceTexture texture, boolean releaseOld) {
        if (customTextureView != null) {
            SurfaceTexture old = getPersistTexture();
            if (customTextureView.setPersistTexture(texture)) {
                // Changed
                if (releaseOld && old != null) {
                    // We manage the texture...
                    old.release();
                    surface = null;
                }
            }
        }
    }

    private SurfaceTexture getPersistTexture() {
        return (customTextureView != null) ? customTextureView.getPersistTexture() : null;
    }

    /**
     * Call this to persist and re-use the surface texture initially created by the
     * TextureView. Otherwise every time we set a new texture there is a delay while
     * decoders re-initialize for the new texture. (This delay occurs even if we
     * re-set the same texture after setting a null texture in between.)
     *
     * In final cleanup this should be called with false.
     *
     * @param persist
     */
    public void setPersistingTexture(boolean persist) {
        if (!persist) {
            // Release current surface if not active since we manage. Other
            setPersistTexture(null, !surfaceActive);
        }
        isPersisting = persist;

    }

    public TextureViewScaleManager(TextureView textureView, SurfaceUser user) {
        this.textureView = textureView;
        this.surfaceUser = user;
        textureView.setSurfaceTextureListener(this);
        if (textureView instanceof CustomTextureView) {
            customTextureView = (CustomTextureView) textureView;
            customTextureView.setOnAttachListener( new CustomTextureView.Listener() {
                @Override
                public void onCustomTextureViewAttached() {
                    // Once we set the texture in onAttachedToWindow we stop getting onSurfaceTextureAvailable
                    SurfaceTexture texture = customTextureView.getSurfaceTexture();
                    updateTexture(texture);
                    // Only set surface active if we got the texture via onSurfaceTextureAvailable
                    surfaceActive = texture != null;
                    if (surfaceUser != null) {
                        surfaceUser.setSurface(surface);
                    }
                    updateMatrix(sourceWidth, sourceHeight, customTextureView.getWidth(), customTextureView.getHeight());
                }
            });
        }
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

    private boolean surfaceActive = false;

    private void updateTexture(SurfaceTexture texture) {
        if (isPersisting) {
            SurfaceTexture oldTexture = getPersistTexture();
            if (texture != oldTexture) {
                //Log.d(ReactVideoViewManager.REACT_CLASS, "updateTexture(): new surface (texture has changed)");
                setPersistTexture(texture, true);
                if (texture != null) {
                    surface = new Surface(texture);
                } else {
                    surface = null;
                }
            }
        } else {
            if (texture != null) {
                surface = new Surface(texture);
            } else {
                surface = null;
            }
        }
    }
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        Log.d(ReactVideoViewManager.REACT_CLASS, "onSurfaceTextureAvailable(): st: " + surfaceTexture);
        surfaceActive = true;
        updateTexture(surfaceTexture);
        if (surfaceUser != null) {
            surfaceUser.setSurface(surface);
        }
        updateMatrix(sourceWidth, sourceHeight, width, height);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
        Log.d(ReactVideoViewManager.REACT_CLASS, "onSurfaceTextureSizeChanged(): st: " + surfaceTexture);
        updateMatrix(sourceWidth, sourceHeight);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        Log.d(ReactVideoViewManager.REACT_CLASS, "onSurfaceTextureDestroyed(): st: " + surfaceTexture);

        if (!isPersisting) {
            surface = null;
            if (surfaceUser != null) {
                surfaceUser.setSurface(null);
            }
        } // else assume TextureView is being re-attached (full screen switch)

        surfaceActive = false;

        // Return true so texture is released in TextureView
        return !isPersisting;
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