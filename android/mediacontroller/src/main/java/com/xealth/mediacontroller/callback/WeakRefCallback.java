package com.xealth.mediacontroller.callback;

import android.os.Handler;

import com.xealth.mediacontroller.MediaControllerView;

import java.lang.ref.WeakReference;


/**
 * Holds weak reference to runnable so inner class reference (held by DoRunnable) can be cleaned up promptly
 * in case where message queue holds on to pending messages
 */
public class WeakRefCallback extends Callback {

    public interface DoRunnable {
        boolean doRun();
    }

    private final WeakReference<DoRunnable> runnable;

    public WeakRefCallback(Handler handler, int timeout,DoRunnable runnable) {
        super(handler, timeout);
        this.runnable = new WeakReference<DoRunnable>(runnable);
    }

    @Override
    protected boolean doRun() {
        DoRunnable r = runnable.get();
        return (r != null) ? r.doRun() : false;
    }
}
