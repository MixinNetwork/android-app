package one.mixin.android.util.mlkit.scan.analyze

import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF

object ScanCoordinateMapper {
    fun transform(
        rect: Rect,
        sourceWidth: Int,
        sourceHeight: Int,
        destWidth: Int,
        destHeight: Int,
    ): RectF {
        val ratio = maxOf(destWidth.toFloat() / sourceWidth, destHeight.toFloat() / sourceHeight)
        val leftOffset = (sourceWidth * ratio - destWidth) / 2f
        val topOffset = (sourceHeight * ratio - destHeight) / 2f
        return RectF(
            rect.left * ratio - leftOffset,
            rect.top * ratio - topOffset,
            rect.right * ratio - leftOffset,
            rect.bottom * ratio - topOffset,
        )
    }

    fun transformCenter(
        rect: Rect,
        sourceWidth: Int,
        sourceHeight: Int,
        destWidth: Int,
        destHeight: Int,
    ): Point {
        val mapped = transform(rect, sourceWidth, sourceHeight, destWidth, destHeight)
        return Point(mapped.centerX().toInt(), mapped.centerY().toInt())
    }
}
