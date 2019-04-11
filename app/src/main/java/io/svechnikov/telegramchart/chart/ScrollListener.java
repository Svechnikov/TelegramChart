package io.svechnikov.telegramchart.chart;

import android.view.GestureDetector;
import android.view.MotionEvent;

// todo find a better way of detecting scrolling intent
public class ScrollListener extends GestureDetector.SimpleOnGestureListener {

    private boolean startScroll;

    @Override
    public boolean onScroll(MotionEvent e1,
                            MotionEvent e2,
                            float distanceX,
                            float distanceY) {
        if (Math.abs(distanceY) > 25) {
            startScroll = true;
        }
        return true;
    }

    public boolean shouldStartScrolling() {
        return startScroll;
    }

    public void scrollingStarted() {
        startScroll = false;
    }
}