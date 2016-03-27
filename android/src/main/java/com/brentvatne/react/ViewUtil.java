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
            forceLayout(view);
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

    public static void invalidateRoot(View view) {
        if (view == null) {
            return;
        }
        View top = view;
        ViewParent parent = view.getParent();
        while (parent != null) {
            if (parent instanceof ViewGroup) {
                top = (ViewGroup) parent;
                parent = parent.getParent();
            } else {
                break;
            }
        }
        top.invalidate();
    }


    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static void dumpChild(View view) {
        if (view == null) {
            return;
        }
        boolean isVis = view.getVisibility() == View.VISIBLE;
        Log.d(ReactVideoViewManager.REACT_CLASS, "ViewUtil.dump(): " +
                view.getClass().getSimpleName() + ": " + "; mw: "
                    + view.getMeasuredWidth() + "; mh: " + view.getMeasuredHeight() + "; att: "
                + view.isAttachedToWindow() + "; vis: " + isVis);
        if (isVis && view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            final int count = group.getChildCount();
            for (int idx = 0; idx < count; ++idx) {
                dumpChild(group.getChildAt(idx));
            }
        }
    }

    public static void dump(View view) {
        Log.d(ReactVideoViewManager.REACT_CLASS, "ViewUtil.dump(): "
                + view.getClass().getSimpleName() + "; is shown: " + view.isShown() + "; token: " + view.getWindowToken() + "; is hw: " + view.isHardwareAccelerated());
        dumpChild(view);
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
