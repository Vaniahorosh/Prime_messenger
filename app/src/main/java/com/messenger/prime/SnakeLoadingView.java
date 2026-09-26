package com.messenger.prime;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.FrameLayout;
import com.google.android.material.progressindicator.CircularProgressIndicator;

/**
 * Материальная анимированная змейка-индикатор загрузки (Material Design 3 Indeterminate Circular Progress).
 */
public class SnakeLoadingView extends FrameLayout {

    private CircularProgressIndicator progressIndicator;

    public SnakeLoadingView(Context context) {
        super(context);
        init(context);
    }

    public SnakeLoadingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SnakeLoadingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        progressIndicator = new CircularProgressIndicator(context);
        progressIndicator.setIndeterminate(true);
        progressIndicator.setIndicatorColor(Color.parseColor("#00E676")); // Neon Prime Green
        progressIndicator.setTrackThickness((int) (3 * getResources().getDisplayMetrics().density));
        progressIndicator.setIndicatorSize((int) (24 * getResources().getDisplayMetrics().density));

        LayoutParams lp = new LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        );
        lp.gravity = Gravity.CENTER;
        addView(progressIndicator, lp);
    }

    public void setIndicatorColor(int color) {
        if (progressIndicator != null) {
            progressIndicator.setIndicatorColor(color);
        }
    }
}
