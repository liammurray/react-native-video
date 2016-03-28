package com.brentvatne.react;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;
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
            //forceLayout(view);
        }

    }
    public static void forceLayout(View view) {
        view.forceLayout();
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            final int count = group.getChildCount();
            for (int idx = 0; idx < count; ++idx) {
                forceLayout(group.getChildAt(idx));
            }
        }
    }

    /**
     * Debug helper for Override onMeasure, copy the following line and log it.
     *
     * describeMeasureInfo(this, getSuggestedMinimumWidth(), getSuggestedMinimumHeight(), widthMeasureSpec, heightMeasureSpec);
     *
     * Log.d(TAG, "onMeasure(): " + ViewUtil.describeMeasureInfo(this, getSuggestedMinimumWidth(), getSuggestedMinimumHeight(), widthMeasureSpec, heightMeasureSpec));
     *
     */
    public static String describeMeasureInfo(View view, int minWidth,
                                      int minHeight, int widthMeasureSpec,
                                      int heightMeasureSpec){
        final int specWidth = MeasureSpec.getSize(widthMeasureSpec);
        final int specHeight = MeasureSpec.getSize(heightMeasureSpec);
        return "min: (" + minWidth + "," + minHeight + "); spec: (" + specWidth + "," + specHeight + "); meas: (" + view.getMeasuredWidth() + "," + view.getMeasuredHeight() +")";

    }

    public static String describeSize(int left, int top, int right, int bottom) {
        return "(" + left + "," + top + "," + right + "," + bottom + ")";
    }

    public static String describeSize(View view) {
        return "(" + view.getLeft() + "," + view.getTop() + "," + view.getRight() + "," + view.getBottom() + ")";
    }
}
