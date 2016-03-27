package com.brentvatne.react.com.brentvatne.react.exoplayer;

import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import com.brentvatne.react.ReactVideoViewManager;
import com.brentvatne.react.ViewUtil;
import com.yqritc.scalablevideoview.ScalableType;
import com.yqritc.scalablevideoview.ScaleManager;
import com.yqritc.scalablevideoview.Size;


public class TextureViewScaleManager implements TextureView.SurfaceTextureListener {

    private ScalableType mScalableType = ScalableType.NONE;
    
    private int sourceWidth;
    
    private int sourceHeight;

    private final TextureView textureView;

    private final CustomTextureView customTextureView;

    private final SurfaceUser surfaceUser;

    private SurfaceTexture persistTexture;

    private Surface surface;

    private boolean isPersisting = true;

    /** True while TextureView is attached */
    private boolean isActive = false;

    public Surface getSurface() {
        return surface;
    }

    public interface SurfaceUser {
        void setSurface(Surface surface);
    }

    private void releaseSurface() {
        if (persistTexture != null) {
            Log.d(ReactVideoViewManager.REACT_CLASS, "releaseSurface(): release surface texture");
            persistTexture.release();
            persistTexture = null;
            surfaceUser.setSurface(null);
        }
        surface = null;
    }



    /**
     * Call this to re-use the texture when the TextureView re-attaches to a window.
     *
     * @param persist
     */
    public void setPersistingTexture(boolean persist) {
        Log.d(ReactVideoViewManager.REACT_CLASS, "setPersistingTexture(): persist: " + persist);
        if (!persist && !isActive) {
            // We can release now. Otherwise wait for onSurfaceTextureDestroyed() (when window detaches).
            releaseSurface();
        }
        isPersisting = persist;
    }

    private void notifySurfaceIfNeeded(SurfaceTexture surfaceTexture) {
        if (isPersisting && surfaceTexture != persistTexture) {
            throw new AssertionError("surface texture changed while persisting");
        }
        if (surface != null) {
            // Already created Surface and did notification
            return;
        }
        Log.d(ReactVideoViewManager.REACT_CLASS, "notifySurfaceIfNeeded(): notify surface(): " + ViewUtil.describeSize(customTextureView));
        surface = new Surface(surfaceTexture);
        surfaceUser.setSurface(surface);
        updateMatrix(sourceWidth, sourceHeight, customTextureView.getWidth(), customTextureView.getHeight());
    }

    public TextureViewScaleManager(TextureView textureView, SurfaceUser user) {
        this.textureView = textureView;
        this.surfaceUser = user;
        textureView.setSurfaceTextureListener(this);
        customTextureView = (textureView instanceof CustomTextureView) ? (CustomTextureView) textureView : null;
        if (customTextureView != null) {
            // Set custom persistTexture persists across attach/detach) every time TextureView attaches to a window
            customTextureView.setOnAttachListener( new CustomTextureView.Listener() {
                @Override
                public void onCustomTextureViewAttached() {
                    Log.d(ReactVideoViewManager.REACT_CLASS, "onCustomTextureViewAttached(): surface: " + surface);
                    if (persistTexture != null) {
                        if (!isPersisting) {
                            throw new AssertionError("bad state");
                        }
                        customTextureView.setSurfaceTexture(persistTexture);
                        isActive = true;
                    }
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

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        Log.d(ReactVideoViewManager.REACT_CLASS, "onSurfaceTextureAvailable(): st: " + surfaceTexture);
        // We do not receive this callback if we set the texture when the window is attached
        if (persistTexture != null || surface != null) {
            throw new AssertionError("bad state");
        }
        if (isPersisting) {
            // First time window is attached. Start persisting...
            persistTexture = surfaceTexture;
        }
        notifySurfaceIfNeeded(surfaceTexture);
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        notifySurfaceIfNeeded(surfaceTexture);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
        Log.d(ReactVideoViewManager.REACT_CLASS, "onSurfaceTextureSizeChanged(): st: " + surfaceTexture);
        updateMatrix(sourceWidth, sourceHeight);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        Log.d(ReactVideoViewManager.REACT_CLASS, "onSurfaceTextureDestroyed(): st: " + surfaceTexture);
        isActive = false;
        if (!isPersisting) {
            // We stopped persisting while active. Now cleanup.
            releaseSurface();
        }
        return false;

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