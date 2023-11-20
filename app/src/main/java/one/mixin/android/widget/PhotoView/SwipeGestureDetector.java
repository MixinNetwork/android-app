package one.mixin.android.widget.PhotoView;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import androidx.annotation.NonNull;
import androidx.core.view.MotionEventCompat;

class SwipeGestureDetector {
    static final int DIRECTION_TOP_BOTTOM = 1;
    static final int DIRECTION_LEFT_RIGHT = 4;

    private OnSwipeGestureListener listener;
    private int direction;
    private int touchSlop;
    private float initialMotionX, initialMotionY;
    private float lastMotionX, lastMotionY;
    private boolean isBeingDragged;

    SwipeGestureDetector(Context context, @NonNull OnSwipeGestureListener listener) {
        this.listener = listener;
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    boolean onInterceptTouchEvent(MotionEvent event) {
        //noinspection deprecation
        int action = MotionEventCompat.getActionMasked(event);

        float x = event.getRawX();
        float y = event.getRawY();

        // Always take care of the touch gesture being complete.
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            // Release the drag.
            reset(initialMotionX, initialMotionY);
            return false;
        }

        if (action != MotionEvent.ACTION_DOWN) {
            if (isBeingDragged) {
                return true;
            }
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                initialMotionX = lastMotionX = x;
                initialMotionY = lastMotionY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                final float xDiff = Math.abs(x - initialMotionX);
                final float yDiff = Math.abs(y - initialMotionY);
                if (xDiff > touchSlop && xDiff > yDiff) {
                    isBeingDragged = true;
                    //direction horizon
                    direction = DIRECTION_LEFT_RIGHT;
                } else if (yDiff > touchSlop && yDiff > xDiff) {
                    isBeingDragged = true;
                    //direction vertical
                    direction = DIRECTION_TOP_BOTTOM;
                }
                break;
        }
        return isBeingDragged;
    }

    boolean onTouchEvent(MotionEvent event) {
        //noinspection deprecation
        int action = MotionEventCompat.getActionMasked(event);
        float x = event.getRawX();
        float y = event.getRawY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                initialMotionX = lastMotionX = x;
                initialMotionY = lastMotionY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                final float deltaX = x - lastMotionX;
                final float deltaY = y - lastMotionY;
                lastMotionX = x;
                lastMotionY = y;
                if (isBeingDragged) {
                    if (direction == DIRECTION_LEFT_RIGHT) {
                        listener.onSwipeLeftRight(deltaX, deltaY);
                    } else if (direction == DIRECTION_TOP_BOTTOM) {
                        listener.onSwipeTopBottom(deltaX, deltaY);
                    }
                } else {
                    final float xDiff = Math.abs(x - initialMotionX);
                    final float yDiff = Math.abs(y - initialMotionY);
                    if (xDiff > touchSlop && xDiff > yDiff) {
                        isBeingDragged = true;
                        //direction horizon
                        direction = DIRECTION_LEFT_RIGHT;
                    } else if (yDiff > touchSlop && yDiff > xDiff) {
                        isBeingDragged = true;
                        //direction vertical
                        direction = DIRECTION_TOP_BOTTOM;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                reset(x, y);
                break;
            case MotionEvent.ACTION_CANCEL:
                reset(x, y);
                break;
        }
        return true;
    }

    int getDirection() {
        return direction;
    }

    private void reset(float x, float y) {
        if (isBeingDragged) {
            listener.onFinish(direction, x - initialMotionX, y - initialMotionY);
        }
        listener.onReset();
        isBeingDragged = false;
    }

    public interface OnSwipeGestureListener {
        void onSwipeTopBottom(float deltaX, float deltaY);

        void onSwipeLeftRight(float deltaX, float deltaY);

        void onFinish(int direction, float distanceX, float distanceY);

        void onReset();
    }

}
