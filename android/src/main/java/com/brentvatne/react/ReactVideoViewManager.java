package com.brentvatne.react;

import com.brentvatne.react.ReactVideoView.Events;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.yqritc.scalablevideoview.ScalableType;

import java.util.Map;

import javax.annotation.Nullable;

public class ReactVideoViewManager extends SimpleViewManager<ReactVideoViewContainer> {

    public static final String REACT_CLASS = "RCTVideo";

    public static final String PROP_SRC = "src";
    public static final String PROP_SRC_URI = "uri";
    public static final String PROP_SRC_TYPE = "type";
    public static final String PROP_SRC_IS_NETWORK = "isNetwork";
    public static final String PROP_SRC_IS_ASSET = "isAsset";
    public static final String PROP_RESIZE_MODE = "resizeMode";
    public static final String PROP_REPEAT = "repeat";
    public static final String PROP_PAUSED = "paused";
    public static final String PROP_MUTED = "muted";
    public static final String PROP_VOLUME = "volume";
    public static final String PROP_SEEK = "seek";
    public static final String PROP_RATE = "rate";
    /** Enable MediaController */
    public static final String PROP_CONTROLS = "controls";
    /** Automatically hide navigation UI (currently only when MediaController enabled) */
    public static final String PROP_AUTOHIDE_NAV = "autoHideNav";

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    protected ReactVideoViewContainer createViewInstance(ThemedReactContext themedReactContext) {
        return new ReactVideoViewContainer(themedReactContext);
    }

    @Override
    @Nullable
    public Map getExportedCustomDirectEventTypeConstants() {
        MapBuilder.Builder builder = MapBuilder.builder();
        for (Events event : Events.values()) {
            builder.put(event.toString(), MapBuilder.of("registrationName", event.toString()));
        }
        return builder.build();
    }

    @Override
    @Nullable
    public Map getExportedViewConstants() {
        return MapBuilder.of(
                "ScaleNone", Integer.toString(ScalableType.LEFT_TOP.ordinal()),
                "ScaleToFill", Integer.toString(ScalableType.FIT_XY.ordinal()),
                "ScaleAspectFit", Integer.toString(ScalableType.FIT_CENTER.ordinal()),
                "ScaleAspectFill", Integer.toString(ScalableType.CENTER_CROP.ordinal())
        );
    }

    @ReactProp(name = PROP_SRC)
    public void setSrc(final ReactVideoViewContainer videoView, @Nullable ReadableMap src) {
        videoView.getVideoView().setSrc(
                src.getString(PROP_SRC_URI),
                src.getString(PROP_SRC_TYPE),
                src.getBoolean(PROP_SRC_IS_NETWORK),
                src.getBoolean(PROP_SRC_IS_ASSET)
        );
    }

    @ReactProp(name = PROP_RESIZE_MODE)
    public void setResizeMode(final ReactVideoViewContainer videoView, final String resizeModeOrdinalString) {
        videoView.getVideoView().setResizeModeModifier(ScalableType.values()[Integer.parseInt(resizeModeOrdinalString)]);
    }

    @ReactProp(name = PROP_REPEAT, defaultBoolean = false)
    public void setRepeat(final ReactVideoViewContainer videoView, final boolean repeat) {
        videoView.getVideoView().setRepeatModifier(repeat);
    }

    @ReactProp(name = PROP_PAUSED, defaultBoolean = false)
    public void setPaused(final ReactVideoViewContainer videoView, final boolean paused) {
        videoView.getVideoView().setPausedModifier(paused);
    }

    @ReactProp(name = PROP_MUTED, defaultBoolean = false)
    public void setMuted(final ReactVideoViewContainer videoView, final boolean muted) {
        videoView.getVideoView().setMutedModifier(muted);
    }

    @ReactProp(name = PROP_VOLUME, defaultFloat = 1.0f)
    public void setVolume(final ReactVideoViewContainer videoView, final float volume) {
        videoView.getVideoView().setVolumeModifier(volume);
    }

    @ReactProp(name = PROP_SEEK)
    public void setSeek(final ReactVideoViewContainer videoView, final float seek) {
        videoView.getVideoView().seekTo(Math.round(seek * 1000.0f));
    }

    @ReactProp(name = PROP_RATE)
    public void setRate(final ReactVideoViewContainer videoView, final float rate) {
        videoView.getVideoView().setRateModifier(rate);
    }

    @ReactProp(name = PROP_CONTROLS, defaultBoolean = true)
    public void setControls(final ReactVideoViewContainer videoView, final boolean showControls) {
        videoView.getVideoView().setShowControls(showControls);
    }

    @ReactProp(name = PROP_AUTOHIDE_NAV, defaultBoolean = true)
    public void setAutoHideNav(final ReactVideoViewContainer videoView, final boolean autoHideNav) {
        videoView.getVideoView().setAutoHideNav(autoHideNav);
    }

}
