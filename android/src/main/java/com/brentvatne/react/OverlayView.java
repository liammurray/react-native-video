package com.brentvatne.react;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;

import com.brentvatne.RCTVideo.R;


/**
 * This is a view that is dynamically inserted into the DecorView frame so that
 * it displays on top of the prior sibling view(s).
 */
public final class OverlayView extends FrameLayout {

    /** ID for ViewGroup into which we insert ourselves */
    private int parentViewGroupId;

    public OverlayView(final Context context, final AttributeSet attrs) {
        this(context, attrs, android.R.id.content);
    }

    public OverlayView(final Context context, final AttributeSet attrs, int parentViewGroupId) {
        super(context, attrs);
        this.parentViewGroupId = parentViewGroupId;
        setId(R.id.overlayView);
    }

    public static OverlayView getOverlay(View view) {
        final View rootView = view.getRootView();
        View out = rootView.findViewById(R.id.overlayView);
        if (out != null && !(out instanceof OverlayView) ) {
            throw new IllegalStateException("Unexpected view type ");
        }
        return (OverlayView)out;
    }


    /**
     * Ensures overlay is added to view hierarchy associated with given view.
     * The view must already be added to decor view hierarchy and attached to window.
     */
    public void attach(View view) {
        if (view.getWindowToken() != null) {
            ensureOverlayAddedToContentFrame(view);
        } else {
            throw new IllegalStateException("Target view has no window");
        }
    }

    /**
     * Removes overlay from content frame and disassociates with target view. 
     * 
     * Do not call this in View.onDetachedFromWindow() since the parent 
     * ViewGroup may be in middle of iterating over hierarchy.
     * See ViewGroup.dispatchDetachedFromWindow().
     */
    public void detach() {
        ViewUtil.detachFromParent(this);
    }

    private ViewGroup findParentGroup(View view) {
        // Add overlay to id/content view. By default it covers content view.
        //
        // The decor view hierarchy looks like this:
        //
        // PhoneWindow.DecorView (FrameLayout)
        // - LinearLayout
        // - - (other decoration)
        // - - FrameLayout (id/content) <-- Add as child to this view
        // - - (other decoration)
        //
        // We add ourselves last (top of z-order) in the id/content frame
        //

        final View rootView = view.getRootView();
        ViewGroup parentGroup = null;
        if (parentViewGroupId == 0 && rootView instanceof FrameLayout) {
            // Add ourselves at root view level
            parentGroup = ((FrameLayout)rootView);
        } else {
            parentGroup = (ViewGroup) rootView.findViewById(parentViewGroupId != 0 ? parentViewGroupId : android.R.id.content);
        }
        return parentGroup;
    }

    /**
     * Ensures overlay is parented to id/content within decor view associated with given view
     * @param view some view that is part of decor view hierarchy
     */
    private boolean ensureOverlayAddedToContentFrame(View view) {
        boolean added = false;
        // If we have a parent we assume it is id/content from previous call
        ViewParent parent = getParent();
        if (parent == null) {
            ViewGroup parentGroup = findParentGroup(view);
            if (parentGroup != null) {
                // Request MATCH_PARENT so we will fill parent area
                parentGroup.addView(this, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
                added = true;
            } else {
                throw new IllegalStateException("Unable to find parent for overlay");
            }
        }
        return added;
    }


}
