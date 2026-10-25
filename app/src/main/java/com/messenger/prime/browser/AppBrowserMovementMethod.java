package com.messenger.prime.browser;

import android.text.Layout;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.MotionEvent;
import android.widget.TextView;

public class AppBrowserMovementMethod extends LinkMovementMethod {
    private static AppBrowserMovementMethod instance;

    public static AppBrowserMovementMethod getInstance() {
        if (instance == null) {
            instance = new AppBrowserMovementMethod();
        }
        return instance;
    }

    @Override
    public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_UP) {
            int x = (int) event.getX() - widget.getTotalPaddingLeft() + widget.getScrollX();
            int y = (int) event.getY() - widget.getTotalPaddingTop() + widget.getScrollY();

            Layout layout = widget.getLayout();
            if (layout != null) {
                int line = layout.getLineForVertical(y);
                float lineLeft = layout.getLineLeft(line);
                float lineRight = layout.getLineRight(line);

                if (x >= lineLeft && x <= lineRight) {
                    int off = layout.getOffsetForHorizontal(line, x);

                    URLSpan[] link = buffer.getSpans(off, off, URLSpan.class);
                    if (link.length != 0) {
                        int spanStart = buffer.getSpanStart(link[0]);
                        int spanEnd = buffer.getSpanEnd(link[0]);
                        if (off >= spanStart && off <= spanEnd) {
                            String url = link[0].getURL();
                            AppBrowserManager.openUrl(widget.getContext(), url);
                            return true;
                        }
                    }
                }
            }
        }
        return super.onTouchEvent(widget, buffer, event);
    }
}
