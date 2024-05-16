package one.mixin.android.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.CornerPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.TransformationUtils
import java.security.MessageDigest
import kotlin.math.cos
import kotlin.math.sin

class GlideRoundedHexagonTransformation : BitmapTransformation() {
    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update("RoundedHexagonTransformation".toByteArray())
    }

    override fun equals(other: Any?): Boolean {
        return other is GlideRoundedHexagonTransformation
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    override fun transform(
        context: Context,
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int,
    ): Bitmap {
        val bitmap = TransformationUtils.centerCrop(pool, toTransform, outWidth, outHeight)

        val path = Path()
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }

        createHexagonPath(bitmap.width.toFloat(), path)

        val result = pool.get(bitmap.width, bitmap.height, toTransform.config)
        paint.pathEffect = CornerPathEffect(result.width / 10f)

        val canvas = Canvas(result)
        canvas.drawPath(path, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return result
    }

    private fun createHexagonPath(
        height: Float,
        path: Path,
    ): Path {
        val radius = height / 2f

        val angleDeg = 60f
        val angleRad = Math.toRadians(angleDeg.toDouble())

        val points = arrayOfNulls<PointF>(6)
        for (i in 0..5) {
            val x = (radius + radius * cos(i * angleRad)).toFloat()
            val y = (radius + radius * sin(i * angleRad)).toFloat()
            points[i] = PointF(x, y)
        }

        path.moveTo(points[0]!!.x, points[0]!!.y)
        for (i in 1..5) {
            path.lineTo(points[i]!!.x, points[i]!!.y)
        }
        path.close()

        return path
    }
}
