package one.mixin.android.widget

import android.animation.ValueAnimator
import android.animation.ValueAnimator.INFINITE
import android.animation.ValueAnimator.REVERSE
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import one.mixin.android.R
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.dp

class BlinkRingView(context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {

    private val initW = 8.dp.toFloat()

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = initW
    }
    private val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = context.colorFromAttribute(R.attr.bg_white)
    }

    private var anim: ValueAnimator = ValueAnimator.ofFloat(0f, initW).apply {
        duration = 500
        addUpdateListener { va ->
            val w = va.animatedValue as Float
            ringPaint.strokeWidth = w
            invalidate()
        }
        repeatMode = REVERSE
        repeatCount = INFINITE
    }

    fun setColor(@ColorInt color: Int) {
        ringPaint.color = color
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        if (visibility == VISIBLE) {
            anim.start()
        } else {
            anim.cancel()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawCircle(width / 2.0f, height / 2.0f, (width - initW) / 2.0f, ringPaint)
        canvas.drawCircle(width / 2.0f, height / 2.0f, (width - initW) / 2.0f, innerPaint)
    }
}
