package one.mixin.android.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class BorderTextView(context: Context, attrs: AttributeSet?) : AppCompatTextView(context, attrs) {
    companion object {
        const val DEFAULT_COLOR = Color.WHITE
        const val DEFAULT_WIDTH = 0F
    }

    private var borderPaint: Paint? = null
    private val bgPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = DEFAULT_COLOR
    }

    override fun onDraw(canvas: Canvas) {
        val borderPaint = borderPaint ?: return
        val w = width
        val h = height
        val radius = if (h > w) h / 2f else w / 2f
        canvas.drawCircle(radius, radius, radius, bgPaint)
        canvas.drawCircle(radius, radius, radius - borderPaint.strokeWidth / 2 + 1, borderPaint)
        super.onDraw(canvas)
    }

    override fun setBackgroundColor(color: Int) {
        bgPaint.color = color
    }

    fun setBorderInfo(borderWidth: Float?, borderColor: Int?) {
        borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = borderColor ?: DEFAULT_COLOR
            strokeWidth = borderWidth ?: DEFAULT_WIDTH
        }
    }
}
