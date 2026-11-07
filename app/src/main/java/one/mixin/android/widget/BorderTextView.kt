package one.mixin.android.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatTextView

class BorderTextView(context: Context, attrs: AttributeSet?) : AppCompatTextView(context, attrs) {
    companion object {
        const val DEFAULT_COLOR = Color.WHITE
        const val DEFAULT_WIDTH = 0F
    }

    private var requestedTextSize: Float? = null

    override fun setTextSize(unit: Int, size: Float) {
        requestedTextSize = TypedValue.applyDimension(unit, size, resources.displayMetrics)
        super.setTextSize(unit, size)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val availableWidth = (measuredWidth - paddingLeft - paddingRight).coerceAtLeast(0).toFloat()
        val availableHeight = (measuredHeight - paddingTop - paddingBottom).coerceAtLeast(0).toFloat()
        val diameter = minOf(availableWidth, availableHeight)
        if (diameter <= 0f) return
        val targetSize = requestedTextSize ?: (diameter * 22f / 48f)
        val textPaint = Paint(paint).apply { textSize = targetSize }
        val metrics = textPaint.fontMetrics
        val scale = minOf(
            1f,
            diameter * 0.8f / textPaint.measureText(text.toString()).coerceAtLeast(1f),
            diameter * 0.8f / (metrics.bottom - metrics.top).coerceAtLeast(1f),
        )
        val fittedSize = targetSize * scale
        if (textSize != fittedSize) {
            super.setTextSize(TypedValue.COMPLEX_UNIT_PX, fittedSize)
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    private var borderPaint: Paint? = null
    private val bgPaint: Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = DEFAULT_COLOR
        }

    override fun onDraw(canvas: Canvas) {
        val w = (width - paddingLeft - paddingRight).coerceAtLeast(0)
        val h = (height - paddingTop - paddingBottom).coerceAtLeast(0)
        val centerX = paddingLeft + w / 2f
        val centerY = paddingTop + h / 2f
        val radius = minOf(w, h) / 2f
        canvas.drawCircle(centerX, centerY, radius, bgPaint)
        borderPaint?.let { borderPaint ->
            canvas.drawCircle(centerX, centerY, (radius - borderPaint.strokeWidth / 2).coerceAtLeast(0f), borderPaint)
        }
        super.onDraw(canvas)
    }

    override fun setBackgroundColor(color: Int) {
        bgPaint.color = color
        invalidate()
    }

    fun setBorderInfo(
        borderWidth: Float?,
        borderColor: Int?,
    ) {
        borderPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = borderColor ?: DEFAULT_COLOR
                strokeWidth = borderWidth ?: DEFAULT_WIDTH
            }
    }
}
