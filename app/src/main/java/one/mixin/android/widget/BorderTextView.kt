package one.mixin.android.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.appcompat.widget.AppCompatTextView
import android.util.AttributeSet

class BorderTextView(context: Context, attrs: AttributeSet?) : AppCompatTextView(context, attrs) {
    companion object {
        const val DEFAULT_COLOR = Color.WHITE
        const val DEFAULT_WIDTH = 0F
    }

    private var borderPaint: Paint? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        borderPaint?.let {
            val w = width
            val h = height
            val radius = if (h > w) h / 2f else w / 2f
            canvas.drawCircle(radius, radius, radius - it.strokeWidth / 2 + 1, it)
        }
    }

    fun setBorderInfo(borderWidth: Float?, borderColor: Int?) {
        borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = borderColor ?: DEFAULT_COLOR
            strokeWidth = borderWidth ?: DEFAULT_WIDTH
        }
    }
}
