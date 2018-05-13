package one.mixin.android.widget.linktext;

import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.view.MotionEvent;
import android.widget.TextView;


class LinkTouchMovementMethod extends LinkMovementMethod {

    private TouchableSpan pressedSpan;

    @Override
    public boolean onTouchEvent(TextView textView, final Spannable spannable, MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            pressedSpan = getPressedSpan(textView, spannable, event);
            if (pressedSpan != null) {
                pressedSpan.setPressed(true);
                Selection.setSelection(spannable, spannable.getSpanStart(pressedSpan),
                        spannable.getSpanEnd(pressedSpan));
                record(textView);
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            TouchableSpan touchedSpan = getPressedSpan(textView, spannable, event);
            if (pressedSpan != null && touchedSpan != pressedSpan) {
                pressedSpan.setPressed(false);
                pressedSpan = null;
                Selection.removeSelection(spannable);
                record(textView);
            }
        } else {
            if (pressedSpan != null) {
                pressedSpan.setPressed(false);
                super.onTouchEvent(textView, spannable, event);
                record(textView);
            }
            pressedSpan = null;
            Selection.removeSelection(spannable);
        }
        return true;
    }

    private void record(TextView textView) {
        if (textView instanceof AutoLinkTextView) {
            ((AutoLinkTextView) textView).clickTime = System.currentTimeMillis();
        }
    }

    private TouchableSpan getPressedSpan(TextView textView, Spannable spannable, MotionEvent event) {

        int x = (int) event.getX();
        int y = (int) event.getY();

        x -= textView.getTotalPaddingLeft();
        y -= textView.getTotalPaddingTop();

        x += textView.getScrollX();
        y += textView.getScrollY();

        Layout layout = textView.getLayout();
        int verticalLine = layout.getLineForVertical(y);
        int horizontalOffset = layout.getOffsetForHorizontal(verticalLine, x);

        TouchableSpan[] link = spannable.getSpans(horizontalOffset, horizontalOffset, TouchableSpan.class);
        TouchableSpan touchedSpan = null;
        if (link.length > 0) {
            touchedSpan = link[0];
        }
        return touchedSpan;
    }
}
