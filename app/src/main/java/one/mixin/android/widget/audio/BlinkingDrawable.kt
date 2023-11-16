package one.mixin.android.widget.audio

import android.animation.ObjectAnimator
import android.animation.ValueAnimator.INFINITE
import android.animation.ValueAnimator.REVERSE
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable

class BlinkingDrawable(private val color: Int) : Drawable() {
    private val bounds = RectF()
    private var w = 0f
    private var h = 0f

    private var alphaAnimator: ObjectAnimator? = null

    private val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = this@BlinkingDrawable.color
            style = Paint.Style.FILL
        }

    override fun onBoundsChange(bounds: Rect) {
        this.bounds.set(bounds)
        w = this.bounds.width()
        h = this.bounds.height()
    }

    override fun draw(canvas: Canvas) {
        canvas.drawOval(0f, 0f, w, h, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        invalidateSelf()
    }

    override fun getOpacity() = PixelFormat.TRANSLUCENT

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    fun blinking() {
        if (alphaAnimator != null) {
            alphaAnimator?.cancel()
        }

        alphaAnimator =
            ObjectAnimator.ofInt(this, "alpha", 255, 0).apply {
                duration = 1000
                repeatMode = REVERSE
                repeatCount = INFINITE
            }
        alphaAnimator?.start()
    }

    fun stopBlinking() {
        alphaAnimator?.cancel()
    }
}
