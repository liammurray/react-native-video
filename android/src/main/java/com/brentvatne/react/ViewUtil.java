package com.brentvatne.react;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.View.MeasureSpec;

public class ViewUtil {

    public static void detachFromParent(View view) {
        if (view != null) {
            ViewParent parent = view.getParent();
            if (parent != null) {
                ((ViewGroup) parent).removeView(view);
            }
        }
    }

}
