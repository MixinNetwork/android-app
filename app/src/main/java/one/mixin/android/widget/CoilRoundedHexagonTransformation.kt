package one.mixin.android.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.CornerPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.core.graphics.createBitmap
import coil.size.Size
import coil.transform.Transformation
import kotlin.math.cos
import kotlin.math.sin

class CoilRoundedHexagonTransformation : Transformation {

    override val cacheKey: String = javaClass.name

    override suspend fun transform(bitmap: Bitmap, size: Size): Bitmap {
        val path = Path()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        createHexagonPath(bitmap.width.toFloat(), path)
        val result = createBitmap(bitmap.width, bitmap.height, bitmap.config)
        paint.pathEffect = CornerPathEffect(result.width /  10f)

        val canvas = Canvas(result)
        canvas.drawPath(path, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return result
    }

    private fun createHexagonPath(height: Float ,path: Path): Path {
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


    override fun equals(other: Any?) = other is CoilRoundedHexagonTransformation

    override fun hashCode() = javaClass.hashCode()
}
