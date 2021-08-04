package one.mixin.android.widget

import android.animation.ValueAnimator
import android.animation.ValueAnimator.INFINITE
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.view.isVisible
import one.mixin.android.R
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.dp
import kotlin.math.max
import kotlin.math.min

class BlinkRingView(context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {

    private val initW = 8.dp.toFloat()
    private val miniW = 1.5f.dp.toFloat()

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 0f
        width
    }
    private val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = context.colorFromAttribute(R.attr.bg_white)
    }

    fun updateAudioLevel(audioLevel: Float) {
        if (audioLevel == 0f) {
            isVisible = false
            return
        } else {
            isVisible = true
        }
        val toValue = max(initW * audioLevel, miniW)
        ValueAnimator.ofFloat(ringPaint.strokeWidth, toValue).apply {
            duration = 200
            addUpdateListener { va ->
                val w = va.animatedValue as Float
                ringPaint.strokeWidth = w
                invalidate()
            }
            repeatCount = INFINITE
        }.start()
    }

    fun setColor(@ColorRes id: Int) {
        ringPaint.color = context.resources.getColor(id, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawCircle(width / 2.0f, height / 2.0f, (width - initW) / 2.0f, ringPaint)
        canvas.drawCircle(width / 2.0f, height / 2.0f, (width - initW) / 2.0f, innerPaint)
    }
}
