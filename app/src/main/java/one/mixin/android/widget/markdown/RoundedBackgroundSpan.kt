package one.mixin.android.widget.markdown

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.FontMetricsInt
import android.graphics.RectF
import android.text.style.ReplacementSpan
import io.noties.markwon.core.MarkwonTheme
import one.mixin.android.extension.dp

class RoundedBackgroundSpan(private val theme: MarkwonTheme) : ReplacementSpan() {
    private val padding by lazy { 1.5f.dp }
    private val topPadding by lazy { 1f.dp }
    private val roundRadius by lazy { 4.dp.toFloat() }

    private var size = 0
    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: FontMetricsInt?
    ): Int {
        size =
            paint.measureText(text.subSequence(start, end).toString()).toInt() + padding + padding
        return size
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
        paint: Paint
    ) {
        canvas.drawText(text, start, end, x + padding, y.toFloat(), paint)
        val rect =
            RectF(x, top.toFloat() + topPadding, x + size, bottom.toFloat())
        paint.color = theme.getCodeBackgroundColor(paint)
        canvas.drawRoundRect(rect, roundRadius, roundRadius, paint)
    }
}
