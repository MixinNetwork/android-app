package one.mixin.android.widget.snow

import android.graphics.Canvas
import android.graphics.Paint
import one.mixin.android.extension.dp
import kotlin.math.cos
import kotlin.math.sin

class SnowParticle(
    val particlePaint: Paint,
    val particleThinPaint: Paint,
    private val angleDiff: Float
) {
    var x = 0f
    var y = 0f
    var vx = 0f
    var vy = 0f
    var velocity = 0f
    var alpha = 0f
    var lifeTime = 0f
    var currentTime = 0f
    var scale = 0f
    var type = 0
    fun draw(canvas: Canvas) {
        when (type) {
            0 -> {
                particlePaint.alpha = (255 * alpha).toInt()
                canvas.drawPoint(x, y, particlePaint)
            }
            1 -> {
                particleThinPaint.alpha = (255 * alpha).toInt()
                var angle = (-Math.PI).toFloat() / 2

                val px = 2.dp * 2 * scale
                val px1 = 0.57f.dp * 2 * scale
                val py1 = 1.55f.dp * 2 * scale
                var a = 0
                while (a < 6) {
                    var x1 = cos(angle.toDouble()).toFloat() * px
                    var y1 = sin(angle.toDouble()).toFloat() * px
                    val cx = x1 * 0.66f
                    val cy = y1 * 0.66f
                    canvas.drawLine(x, y, x + x1, y + y1, particleThinPaint)
                    val angle2 = (angle - Math.PI / 2).toFloat()
                    x1 =
                        (cos(angle2.toDouble()) * px1 - sin(angle2.toDouble()) * py1).toFloat()
                    y1 =
                        (sin(angle2.toDouble()) * px1 + cos(angle2.toDouble()) * py1).toFloat()
                    canvas.drawLine(x + cx, y + cy, x + x1, y + y1, particleThinPaint)
                    x1 =
                        (-cos(angle2.toDouble()) * px1 - sin(angle2.toDouble()) * py1).toFloat()
                    y1 =
                        (-sin(angle2.toDouble()) * px1 + cos(angle2.toDouble()) * py1).toFloat()
                    canvas.drawLine(x + cx, y + cy, x + x1, y + y1, particleThinPaint)
                    angle += angleDiff
                    a++
                }
            }
            else -> {
                particleThinPaint.alpha = (255 * alpha).toInt()
                var angle = (-Math.PI).toFloat() / 2
                val px = 6 * 2 * scale
                val px1 = -3.42f * 2 * scale
                val py1 = 4.65f * 2 * scale
                var a = 0
                while (a < 6) {
                    var x1 = cos(angle.toDouble()).toFloat() * px
                    var y1 = sin(angle.toDouble()).toFloat() * px
                    val cx = x1 * 0.66f
                    val cy = y1 * 0.66f
                    canvas.drawLine(x, y, x + x1, y + y1, particleThinPaint)
                    val angle2 = (angle - Math.PI / 2).toFloat()
                    x1 =
                        (cos(angle2.toDouble()) * px1 - sin(angle2.toDouble()) * py1).toFloat()
                    y1 =
                        (sin(angle2.toDouble()) * px1 + cos(angle2.toDouble()) * py1).toFloat()
                    canvas.drawLine(x + cx, y + cy, x + x1, y + y1, particleThinPaint)
                    x1 =
                        (-cos(angle2.toDouble()) * px1 - sin(angle2.toDouble()) * py1).toFloat()
                    y1 =
                        (-sin(angle2.toDouble()) * px1 + cos(angle2.toDouble()) * py1).toFloat()
                    canvas.drawLine(x + cx, y + cy, x + x1, y + y1, particleThinPaint)
                    angle += angleDiff
                    a++
                }
            }
        }
    }
}
