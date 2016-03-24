package com.xealth.mediacontroller.callback;

import android.os.Handler;
import android.os.SystemClock;

abstract public class Callback implements Runnable {
    protected boolean isPending;
    protected int timeout;
    protected final Handler handler;
    private long nextTime;

    public Callback(Handler handler, int timeout) {
        this.handler = handler;
        this.timeout = timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getTimeout() {
        return timeout;
    }

    public boolean isPending() {
        return isPending;
    }

    public void set() {
        if (!isPending) {
            nextTime = SystemClock.uptimeMillis();
            schedule();
        }
    }

    public void reset() {
        cancel();
        set();
    }

    /** Like reset, but only if timeout if pending */
    public void extend() {
        if (isPending) {
            reset();
        }
    }

    public void cancel() {
        if (isPending) {
            handler.removeCallbacks(this);
            isPending = false;
        }
    }


    public final void run() {
        isPending = false;
        if (doRun()) {
            schedule();
        }
    }

    protected final void schedule() {
        nextTime += timeout;
        handler.postAtTime(this, nextTime);
        isPending = true;
    }

    abstract protected boolean doRun();


}