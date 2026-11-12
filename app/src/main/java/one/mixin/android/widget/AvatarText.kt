package one.mixin.android.widget

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect

internal fun Canvas.drawAvatarText(
    text: String,
    paint: Paint,
    centerX: Float,
    centerY: Float,
    width: Float,
    height: Float,
) {
    if (text.isEmpty()) return
    val bounds = Rect()
    paint.getTextBounds(text, 0, text.length, bounds)
    val scale = minOf(
        1f,
        width * 0.8f / maxOf(paint.measureText(text), bounds.width().toFloat(), 1f),
        height * 0.8f / bounds.height().coerceAtLeast(1),
    )
    paint.textSize *= scale
    paint.getTextBounds(text, 0, text.length, bounds)
    drawText(text, centerX - bounds.exactCenterX(), centerY - bounds.exactCenterY(), paint)
}
