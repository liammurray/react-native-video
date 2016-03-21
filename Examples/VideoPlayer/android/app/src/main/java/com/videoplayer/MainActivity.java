package com.videoplayer;

import android.content.Intent;
import android.content.res.Configuration;
import android.util.Log;

import com.brentvatne.react.OverlayView;
import com.brentvatne.react.ReactVideoViewManager;
import com.facebook.react.ReactActivity;
import com.brentvatne.react.ReactVideoPackage;
import com.facebook.react.ReactPackage;
import com.facebook.react.shell.MainReactPackage;
import com.github.yamill.orientation.OrientationPackage;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends ReactActivity {

    private OverlayView mOverlayView;

    @Override
    protected void onDestroy() {
        Log.d(ReactVideoViewManager.REACT_CLASS, "MainActivity.onDestroy() ");
        super.onDestroy();
    }

    @Override
    public void onAttachedToWindow() {
        Log.d(ReactVideoViewManager.REACT_CLASS, "MainActivity.onAttachedToWindow() ");
        super.onAttachedToWindow();
        ensureOverlayView();
        mOverlayView.attach(getWindow().getDecorView());
    }

    private void ensureOverlayView() {
        if (mOverlayView == null) {
            Log.d(ReactVideoViewManager.REACT_CLASS, "MainActivity.ensureOverlayView() creating overlay");
            mOverlayView = new OverlayView(this, null);
        }
    }

    /**
     * Returns the name of the main component registered from JavaScript.
     * This is used to schedule rendering of the component.
     */
    @Override
    protected String getMainComponentName() {
        return "VideoPlayer";
    }

    /**
     * Returns whether dev mode should be enabled.
     * This enables e.g. the dev menu.
     */
    @Override
    protected boolean getUseDeveloperSupport() {
        return BuildConfig.DEBUG;
    }

   /**
   * A list of packages used by the app. If the app uses additional views
   * or modules besides the default ones, add more packages here.
   */
    @Override
    protected List<ReactPackage> getPackages() {
        Log.d(ReactVideoViewManager.REACT_CLASS, "MainActivity.getPackages() ");
        ensureOverlayView();
        return Arrays.<ReactPackage>asList(
                new MainReactPackage(), new AndroidUtilPackage(), new OrientationPackage(this),
                new ReactVideoPackage(mOverlayView));
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Intent intent = new Intent("onConfigurationChanged");
        intent.putExtra("newConfig", newConfig);
        this.sendBroadcast(intent);
    }
}
