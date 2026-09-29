package com.messenger.prime;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.FrameLayout;

/**
 * Material Design 3 Expressive morphing loader view.
 * Embeds M3ExpressiveLoadingView for expressive shape morphing animations.
 */
public class SnakeLoadingView extends FrameLayout {

    private M3ExpressiveLoadingView expressiveLoadingView;

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
        expressiveLoadingView = new M3ExpressiveLoadingView(context);
        expressiveLoadingView.setColor(Color.parseColor("#00E676")); // Neon Prime Green

        int sizePx = (int) (32 * getResources().getDisplayMetrics().density);
        LayoutParams lp = new LayoutParams(sizePx, sizePx);
        lp.gravity = Gravity.CENTER;
        addView(expressiveLoadingView, lp);
    }

    public void setIndicatorColor(int color) {
        if (expressiveLoadingView != null) {
            expressiveLoadingView.setColor(color);
        }
    }
}
