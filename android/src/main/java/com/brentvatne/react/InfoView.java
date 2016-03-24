package com.brentvatne.react;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.brentvatne.RCTVideo.R;

public class InfoView extends FrameLayout {

    public enum State {
        LOADING,
        HIDDEN,
        FAILED
    }

    private ProgressBar progress;

    private TextView text;

    private State state = State.HIDDEN;

    public InfoView(Context context) {
        super(context);
    }

    public InfoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public InfoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        progress = (ProgressBar)findViewById(R.id.progressbar);
        text = (TextView)findViewById(R.id.text);
    }

    public void setState(State state) {
        this.state = state;
        switch (state) {
            case LOADING:
                progress.setVisibility(View.VISIBLE);
                text.setVisibility(View.GONE);
                setVisibility(View.VISIBLE);
                break;
            case HIDDEN:
                progress.setVisibility(View.GONE);
                text.setVisibility(View.GONE);
                setVisibility(View.GONE);
                break;
            case FAILED:
                progress.setVisibility(View.GONE);
                text.setVisibility(View.VISIBLE);
                text.setText(R.string.video_unavailable);
                setVisibility(View.VISIBLE);
                break;
        }
    }
}
