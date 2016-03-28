package com.brentvatne.react.exoplayer;

import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import com.brentvatne.react.ReactVideoViewManager;
import com.brentvatne.react.ViewUtil;
import com.yqritc.scalablevideoview.ScalableType;
import com.yqritc.scalablevideoview.ScaleManager;
import com.yqritc.scalablevideoview.Size;

/**
 * Manages persisting SurfaceTexture used by TextureView. This makes video playback
 * during fullscreen mode switch (during which time view is reattached within view
 * hierarchy) proceed without a break in continuity.
 *
 * Also manages scale matrix set on TextureView.
 */
public class TextureViewHelper implements TextureView.SurfaceTextureListener {

    private ScalableType mScalableType = ScalableType.NONE;

    private int sourceWidth;
    
    private int sourceHeight;

    private final TextureView textureView;

    private final SurfaceUser surfaceUser;

    private SurfaceTexture persistTexture;

    private Surface surface;

    private boolean isPersisting = true;

    /** True while TextureView is attached to window */
    private boolean isActive = false;

    public Surface getSurface() {
        return surface;
    }

    public interface SurfaceUser {
        void setSurface(Surface surface);
    }

    private void releaseSurface(boolean notifyUser) {
        if (persistTexture != null) {
            Log.d(ReactVideoViewManager.REACT_CLASS, "releaseSurface(): release surface texture");
            persistTexture.release();
            persistTexture = null;
            if (notifyUser) {
                surfaceUser.setSurface(null);
            }
        }
        surface = null;
    }

    private void releaseSurface() {
        releaseSurface(true);
    }

    /**
     * Call this to re-use the texture when the TextureView re-attaches to a window.
     *
     * You should only call this with (false) when you know TextureView will detach (i.e., in final cleanup method)
     *
     * @param persist
     */
    public void setPersistTexture(boolean persist) {
        Log.d(ReactVideoViewManager.REACT_CLASS, "setPersistTexture(): persist: " + persist);
        if (!persist && !isActive) {
            // We can release now. Otherwise wait for onSurfaceTextureDestroyed() (when window detaches).
            releaseSurface();
        }
        isPersisting = persist;
    }

    public boolean isPersisting() {
        return isPersisting;
    }

    //TODO Make this work so we can force new texture on new video playback (old surface flashes old content)
//    public void setNewPersistTexture() {
//        SurfaceTexture surfaceTexture = new SurfaceTexture(0);
//        isPersisting = true;
//        if (isActive) {
//            // releaseSurface(false); <-- Don't do this. TextureView will release the texture in setSurfaceTexture().
//            persistTexture = surfaceTexture;
//            surface = null;
//            customTextureView.setSurfaceTexture(persistTexture);
//            notifySurfaceIfNeeded(surfaceTexture);
//        } else {
//            releaseSurface();
//            persistTexture = surfaceTexture;
//        }
//    }

    private void notifySurfaceIfNeeded(SurfaceTexture surfaceTexture) {
        if (isPersisting && surfaceTexture != persistTexture) {
            Log.d(ReactVideoViewManager.REACT_CLASS, "notifySurfaceIfNeeded(): WARNING: surface texture changed while persisting; old: " + persistTexture + "; new: " + surfaceTexture);
        }
        if (surface != null) {
            // Already created Surface and did notification
            return;
        }
        Log.d(ReactVideoViewManager.REACT_CLASS, "notifySurfaceIfNeeded(): notify surface(): " + ViewUtil.describeSize(textureView));
        surface = new Surface(surfaceTexture);
        surfaceUser.setSurface(surface);
        updateMatrix(sourceWidth, sourceHeight, textureView.getWidth(), textureView.getHeight());
    }

    /**
     * Set custom persistTexture persists across attach/detach) every time TextureView attaches to a window.
     * Once we do this we won't receive a callback to onSurfaceTextureAvailable(). We do get other callbacks.
     */
    private void onTextureViewAttached() {
        Log.d(ReactVideoViewManager.REACT_CLASS, "onTextureViewAttached(): surface: " + surface);
        if (persistTexture != null) {
            if (!isPersisting) {
                throw new AssertionError("bad state");
            }
            textureView.setSurfaceTexture(persistTexture);
            isActive = true;
        }
    }

    public TextureViewHelper(TextureView textureView, SurfaceUser user) {
        this.textureView = textureView;
        this.surfaceUser = user;
        textureView.setSurfaceTextureListener(this);
        textureView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                onTextureViewAttached();
            }

            @Override
            public void onViewDetachedFromWindow(View v) {

            }
        });

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