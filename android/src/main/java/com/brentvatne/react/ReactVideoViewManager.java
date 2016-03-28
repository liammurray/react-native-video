package com.brentvatne.react;

import com.brentvatne.react.ReactVideoViewContainer.Events;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.yqritc.scalablevideoview.ScalableType;

import static com.brentvatne.react.ReactVideoModelState.*;

import java.util.Map;

import javax.annotation.Nullable;

public class ReactVideoViewManager extends SimpleViewManager<ReactVideoHostView> {

    public static final String REACT_CLASS = "RCTVideo";

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    protected ReactVideoHostView createViewInstance(ThemedReactContext themedReactContext) {
        return new ReactVideoHostView(themedReactContext);
    }

    @Override
    @Nullable
    public Map getExportedCustomDirectEventTypeConstants() {
        MapBuilder.Builder builder = MapBuilder.builder();
        for (Events event : ReactVideoViewContainer.Events.values()) {
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
    public void setSrc(final ReactVideoHostView hostView, @Nullable ReadableMap src) {
        hostView.getModelState().setSrc(src);
    }

    @ReactProp(name = PROP_RESIZE_MODE)
    public void setResizeMode(final ReactVideoHostView hostView, final String resizeModeOrdinalString) {
        hostView.getModelState().setResizeMode(resizeModeOrdinalString);
    }

    @ReactProp(name = PROP_REPEAT, defaultBoolean = false)
    public void setRepeat(final ReactVideoHostView hostView, final boolean repeat) {
        hostView.getModelState().setRepeat(repeat);
    }

    @ReactProp(name = PROP_PAUSED, defaultBoolean = false)
    public void setPaused(final ReactVideoHostView hostView, final boolean paused) {
        hostView.getModelState().setPaused(paused);
    }

    @ReactProp(name = PROP_MUTED, defaultBoolean = false)
    public void setMuted(final ReactVideoHostView hostView, final boolean muted) {
        hostView.getModelState().setMuted(muted);
    }

    @ReactProp(name = PROP_VOLUME, defaultFloat = 1.0f)
    public void setVolume(final ReactVideoHostView hostView, final float volume) {
        hostView.getModelState().setVolume(volume);
    }

    @ReactProp(name = PROP_SEEK)
    public void setSeek(final ReactVideoHostView hostView, final float seek) {
        hostView.getModelState().setSeek(seek);
    }

    @ReactProp(name = PROP_RATE)
    public void setRate(final ReactVideoHostView hostView, final float rate) {
        hostView.getModelState().setRate(rate);
    }

    @ReactProp(name = PROP_CONTROLS, defaultBoolean = true)
    public void setControls(final ReactVideoHostView hostView, final boolean showControls) {
        hostView.getModelState().setEnableControls(showControls);
    }

    @ReactProp(name = PROP_AUTOHIDE_NAV, defaultBoolean = true)
    public void setAutoHideNav(final ReactVideoHostView hostView, final boolean autoHideNav) {
        hostView.getModelState().setAutoHideNav(autoHideNav);
    }

}
