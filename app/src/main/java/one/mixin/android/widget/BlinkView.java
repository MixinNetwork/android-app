package one.mixin.android.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;

public class BlinkView extends View {
    private static final int MESSAGE_BLINK = 0x42;
    private static final int BLINK_DELAY = 50;

    private boolean mBlink;
    private boolean mBlinkState;
    private final Handler mHandler;

    public BlinkView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mHandler = new Handler(msg -> {
            if (msg.what == MESSAGE_BLINK) {
                if (mBlink) {
                    float alpha = getAlpha();
                    if (mBlinkState) {
                        if (alpha < 1f) {
                            alpha += 0.1f;
                        } else {
                            mBlinkState = false;
                        }
                    } else {
                        if (alpha > 0f) {
                            alpha -= 0.1f;
                        } else {
                            mBlinkState = true;
                        }
                    }
                    setAlpha(alpha);
                    makeBlink();
                }
                invalidate();
                return true;
            }
            return false;
        });
    }

    private void makeBlink() {
        Message message = mHandler.obtainMessage(MESSAGE_BLINK);
        mHandler.sendMessageDelayed(message, BLINK_DELAY);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mBlink = true;
        mBlinkState = true;
        setAlpha(0f);
        makeBlink();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mHandler.removeMessages(MESSAGE_BLINK);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
    }
}