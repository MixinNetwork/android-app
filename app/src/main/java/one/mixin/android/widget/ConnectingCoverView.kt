package one.mixin.android.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import one.mixin.android.extension.dp

class ConnectingCoverView(context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFF")
    }
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#66000000")
    }
    private val dotRadius = 3.dp.toFloat()
    private val dotInterval = 2.dp
    private val dotOffset = dotRadius * 2 + dotInterval

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width
        val h = height
        canvas.drawCircle(w / 2f, h / 2f, w / 2f, bgPaint)
        canvas.drawCircle(w / 2f - dotOffset, h / 2f, dotRadius, dotPaint)
        canvas.drawCircle(w / 2f, h / 2f, dotRadius, dotPaint)
        canvas.drawCircle(w / 2f + dotOffset, h / 2f, dotRadius, dotPaint)
    }
}
