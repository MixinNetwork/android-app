package one.mixin.android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.core.widget.NestedScrollView;

public class FixedNestedScrollView extends NestedScrollView {

    public FixedNestedScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            // Explicitly call computeScroll() to make the Scroller compute itself
            computeScroll();
        }
        return super.onInterceptTouchEvent(ev);
    }
}