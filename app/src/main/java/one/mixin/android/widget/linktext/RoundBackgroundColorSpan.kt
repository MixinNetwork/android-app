package one.mixin.android.widget.linktext

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.FontMetricsInt
import android.graphics.RectF
import android.text.style.ReplacementSpan
import one.mixin.android.extension.dp

class RoundBackgroundColorSpan(private val bgColor: Int, private val textColor: Int) :
    ReplacementSpan() {
    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: FontMetricsInt?,
    ): Int {
        return (paint.measureText(text, start, end).toInt() + 8.dp)
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
        paint.color = bgColor
        canvas.drawRoundRect(
            RectF(
                x,
                (top + paint.textSize * 0.02).toFloat(),
                x + (paint.measureText(text, start, end).toInt() + 8.dp),
                (bottom - paint.textSize * 0.02).toFloat(),
            ),
            4.dp.toFloat(),
            4.dp.toFloat(),
            paint,
        )
        paint.color = textColor
        canvas.drawText(text, start, end, (x + 4.dp), (y + paint.textSize * 0.02).toFloat(), paint)
        paint.color = originalColor
    }
}
