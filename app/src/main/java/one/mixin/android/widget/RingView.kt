package one.mixin.android.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorRes
import one.mixin.android.R
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.dp

class RingView(context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        width
    }
    private val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = context.colorFromAttribute(R.attr.bg_white)
    }

    fun setColor(@ColorRes id: Int) {
        ringPaint.color = context.resources.getColor(id, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawCircle(width / 2.0f, height / 2.0f, width / 2.0f, ringPaint)
        canvas.drawCircle(width / 2.0f, height / 2.0f, (width - 4.dp) / 2.0f, innerPaint)
    }
}
