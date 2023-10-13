package one.mixin.android.widget;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.animation.AnimationUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import one.mixin.android.widget.keyboard.CubicBezierInterpolator;

public class PlayPauseDrawable extends Drawable {

    private final Paint paint;
    private int size;

    private boolean pause;
    private float progress;
    private long lastUpdateTime;

    private boolean firstTimeNotAnimated = false;
    private boolean first = true;

    public PlayPauseDrawable() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
    }

    public void setSize(int size) {
        this.size = size;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        long newUpdateTime = AnimationUtils.currentAnimationTimeMillis();
        long dt = newUpdateTime - lastUpdateTime;
        lastUpdateTime = newUpdateTime;
        if (dt > 18) {
            dt = 16;
        }
        if (pause && progress < 1f) {
            progress += dt / 300f;
            if (progress >= 1f) {
                progress = 1f;
            } else {
                invalidateSelf();
            }
        } else if (!pause && progress > 0f) {
            progress -= dt / 300f;
            if (progress <= 0f) {
                progress = 0f;
            } else {
                invalidateSelf();
            }
        }
        final Rect bounds = getBounds();
        canvas.save();
        canvas.translate(bounds.centerX() + AndroidUtilities.dp(1) * (1.0f - progress), bounds.centerY());
        final float ms = 500.0f * progress;
        final float rotation;
        if (ms < 100) {
            rotation = -5 * CubicBezierInterpolator.EASE_BOTH.getInterpolation(ms / 100.0f);
        } else if (ms < 484) {
            rotation = -5 + 95 * CubicBezierInterpolator.EASE_BOTH.getInterpolation((ms - 100) / 384);
        } else {
            rotation = 90;
        }
        canvas.scale(1.45f * size / AndroidUtilities.dp(28), 1.5f * size / AndroidUtilities.dp(28));
        canvas.rotate(rotation);
        AndroidUtilitiesKt.getPlayPauseAnimator().draw(canvas, paint, ms);
        canvas.scale(1.0f, -1.0f);
        AndroidUtilitiesKt.getPlayPauseAnimator().draw(canvas, paint, ms);
        canvas.restore();
    }

    public void setFirstTimeNotAnimated(boolean firstTimeNotAnimated) {
        this.firstTimeNotAnimated = firstTimeNotAnimated;
    }

    public void setPause(boolean pause) {
        if (firstTimeNotAnimated && first) {
            setPause(pause, false);
            first = false;
        } else {
            setPause(pause, true);
        }
    }

    public void setPause(boolean pause, boolean animated) {
        if (this.pause != pause) {
            this.pause = pause;
            if (!animated) {
                progress = pause ? 1f : 0f;
            }
            this.lastUpdateTime = AnimationUtils.currentAnimationTimeMillis();
            invalidateSelf();
        }
    }

    @Override
    public void setAlpha(int i) {
        paint.setAlpha(i);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSPARENT;
    }

    @Override
    public int getIntrinsicWidth() {
        return size;
    }

    @Override
    public int getIntrinsicHeight() {
        return size;
    }
}
