package one.mixin.android.widget.linktext

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.FontMetricsInt
import android.graphics.RectF
import android.text.style.ReplacementSpan
import one.mixin.android.extension.dp
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class RoundBackgroundColorSpan(private val bgColor: Int, private val textColor: Int) :
    ReplacementSpan() {
    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: FontMetricsInt?,
    ): Int {
        val metrics = paint.fontMetrics
        fm?.let {
            val ascent = floor(metrics.ascent - 1.dp).toInt()
            val descent = ceil(metrics.descent + 1.dp).toInt()
            it.ascent = min(it.ascent, ascent)
            it.descent = max(it.descent, descent)
            it.top = min(it.top, ascent)
            it.bottom = max(it.bottom, descent)
        }
        return ceil(paint.measureText(text, start, end) + 2 * 5.dp).toInt()
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint,
    ) {
        val originalColor = paint.color
        val metrics = paint.fontMetrics
        val textWidth = paint.measureText(text, start, end)
        val width = ceil(textWidth + 2 * 5.dp)
        paint.color = bgColor
        canvas.drawRoundRect(
            RectF(
                x,
                y + metrics.ascent - 1.dp,
                x + width,
                y + metrics.descent + 1.dp,
            ),
            4.dp.toFloat(),
            4.dp.toFloat(),
            paint,
        )
        paint.color = textColor
        canvas.drawText(text, start, end, x + (width - textWidth) / 2, y.toFloat(), paint)
        paint.color = originalColor
    }
}
