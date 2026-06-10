package one.mixin.android.ui.qr

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import one.mixin.android.extension.dp

class FocusView(context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {
    private val interpolator = DecelerateInterpolator()

    private val outerPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 2.dp.toFloat()
        }
    private val innerPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x7fffffff
        }

    private var focusProgress = 1f
    private var outerAlpha = 1f
    private var innerAlpha = 1f
    private var cx = 0f
    private var cy = 0f
    private var lastDownTime = 0L

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (focusProgress != 1f && innerAlpha != 0f && outerAlpha != 0f) {
            val baseRad = 30.dp
            val newTime = System.currentTimeMillis()
            var dt = newTime - lastDownTime
            if (dt < 0 || dt > 17) {
                dt = 17
            }
            lastDownTime = newTime

            outerPaint.alpha = (interpolator.getInterpolation(outerAlpha) * 255).toInt()
            innerPaint.alpha = (interpolator.getInterpolation(innerAlpha) * 127).toInt()
            val interpolated = interpolator.getInterpolation(focusProgress)
            canvas.drawCircle(cx, cy, baseRad + baseRad * (1f - interpolated), outerPaint)
            canvas.drawCircle(cx, cy, baseRad * interpolated, innerPaint)

            when {
                focusProgress < 1 -> {
                    focusProgress += dt / 300f
                    if (focusProgress > 1) {
                        focusProgress = 1f
                    }
                    invalidate()
                }
                innerAlpha != 0f -> {
                    innerAlpha -= dt / 225f
                    if (innerAlpha < 0) {
                        innerAlpha = 0f
                    }
                    invalidate()
                }
                outerAlpha != 0f -> {
                    outerAlpha -= dt / 225f
                    if (outerAlpha < 0) {
                        outerAlpha = 0f
                    }
                    invalidate()
                }
            }
        }
    }

    fun focusAndMeter(
        x: Float,
        y: Float,
    ) {
        focusProgress = 0f
        outerAlpha = 1f
        innerAlpha = 1f
        cx = x
        cy = y
        lastDownTime = System.currentTimeMillis()
        invalidate()
    }
}
