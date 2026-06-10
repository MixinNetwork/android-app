package one.mixin.android.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import one.mixin.android.extension.dpToPx

class CaptureBorderView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val paint by lazy {
        Paint().apply {
            style = Paint.Style.STROKE
            color = Color.WHITE
            strokeWidth = context.dpToPx(10f).toFloat()
        }
    }

    private val r = Rect()

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        r.set(0, 0, measuredWidth, measuredHeight)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(r, paint)
    }
}
