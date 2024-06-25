package one.mixin.android.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.renderscript.RSRuntimeException
import androidx.core.graphics.createBitmap
import coil.size.Size
import coil.transform.Transformation
import jp.wasabeef.glide.transformations.internal.FastBlur
import jp.wasabeef.glide.transformations.internal.RSBlur

class BlurTransformation(val context: Context, val radius: Int = 25) : Transformation {
    companion object {
        private val MAX_RADIUS: Int = 25
        private val DEFAULT_DOWN_SAMPLING: Int = 1
    }

    override val cacheKey: String = javaClass.name

    override suspend fun transform(
        toTransform: Bitmap,
        size: Size,
    ): Bitmap {
        val width = toTransform.width
        val height = toTransform.height
        val scaledWidth = width / DEFAULT_DOWN_SAMPLING
        val scaledHeight = height / DEFAULT_DOWN_SAMPLING

        var bitmap = createBitmap(toTransform.width, toTransform.height, Bitmap.Config.ARGB_8888)

        bitmap.density = toTransform.getDensity()

        val canvas = Canvas(bitmap)
        canvas.scale(1 / DEFAULT_DOWN_SAMPLING.toFloat(), 1 / DEFAULT_DOWN_SAMPLING.toFloat())
        val paint = Paint()
        paint.flags = Paint.FILTER_BITMAP_FLAG
        canvas.drawBitmap(toTransform, 0f, 0f, paint)

        bitmap =
            try {
                RSBlur.blur(context, bitmap, radius)
            } catch (e: RSRuntimeException) {
                FastBlur.blur(bitmap, radius, true)
            }

        return bitmap
    }

    override fun equals(other: Any?) = other is CoilRoundedHexagonTransformation

    override fun hashCode() = javaClass.hashCode()
}
