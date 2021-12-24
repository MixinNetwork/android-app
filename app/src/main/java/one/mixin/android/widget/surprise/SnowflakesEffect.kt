package one.mixin.android.widget.surprise

import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import one.mixin.android.extension.dp
import java.util.ArrayList
import java.util.Random
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class SnowflakesEffect {
    private val particlePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val particleThinPaint: Paint
    private var lastAnimationTime: Long = 0
    private val random = Random()
    val angleDiff = (PI / 180 * 60).toFloat()

    private inner class Particle {
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
                    var angle = (-PI).toFloat() / 2
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
                        val angle2 = (angle - PI / 2).toFloat()
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
                    var angle = (-PI).toFloat() / 2
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
                        val angle2 = (angle - PI / 2).toFloat()
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

    private val particles = ArrayList<Particle>()
    private val freeParticles = ArrayList<Particle>()
    private var color = 0
    private fun updateColors() {
        val color = -0x19191a
        if (this.color != color) {
            this.color = color
            particlePaint.color = color
            particleThinPaint.color = color
        }
    }

    private fun updateParticles(dt: Long) {
        var count = particles.size
        var a = 0
        while (a < count) {
            val particle = particles[a]
            if (particle.currentTime >= particle.lifeTime) {
                if (freeParticles.size < 40) {
                    freeParticles.add(particle)
                }
                particles.removeAt(a)
                a--
                count--
                a++
                continue
            }
            if (particle.currentTime < 200.0f) {
                particle.alpha =
                    AccelerateInterpolator().getInterpolation(particle.currentTime / 200.0f)
            } else {
                particle.alpha =
                    1.0f - DecelerateInterpolator().getInterpolation((particle.currentTime - 200.0f) / (particle.lifeTime - 200.0f))
            }
            particle.x += particle.vx * particle.velocity * dt / 500.0f
            particle.y += particle.vy * particle.velocity * dt / 500.0f
            particle.currentTime += dt.toFloat()
            a++
        }
    }

    fun onDraw(parent: View?, canvas: Canvas?) {
        if (parent == null || canvas == null) {
            return
        }
        val count = particles.size
        for (a in 0 until count) {
            val particle = particles[a]
            particle.draw(canvas)
        }
        if (random.nextFloat() > 0.7f && particles.size < 100) {
            val cx = random.nextFloat() * parent.measuredWidth
            val cy = random.nextFloat() * (parent.measuredHeight - 20f.dp)
            val angle = random.nextInt(40) - 20 + 90
            val vx = cos(PI / 180.0 * angle).toFloat()
            val vy = sin(PI / 180.0 * angle).toFloat()
            val newParticle: Particle
            if (freeParticles.isNotEmpty()) {
                newParticle = freeParticles[0]
                freeParticles.removeAt(0)
            } else {
                newParticle = Particle()
            }
            newParticle.x = cx
            newParticle.y = cy
            newParticle.vx = vx
            newParticle.vy = vy
            newParticle.alpha = 0.0f
            newParticle.currentTime = 0f
            newParticle.scale = random.nextFloat() * 1.2f
            newParticle.type = random.nextInt(2)
            newParticle.lifeTime = (2000 + random.nextInt(100)).toFloat()
            newParticle.velocity = 20.0f + random.nextFloat() * 4.0f
            particles.add(newParticle)
        }
        val newTime = System.currentTimeMillis()
        val dt = min(17, newTime - lastAnimationTime)
        updateParticles(dt)
        lastAnimationTime = newTime
        parent.invalidate()
    }

    init {
        particlePaint.strokeWidth = 1.5f.dp.toFloat()
        particlePaint.strokeCap = Paint.Cap.ROUND
        particlePaint.style = Paint.Style.STROKE
        particleThinPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        particleThinPaint.strokeWidth = 0.5f.dp.toFloat()
        particleThinPaint.strokeCap = Paint.Cap.ROUND
        particleThinPaint.style = Paint.Style.STROKE
        updateColors()
        repeat(20) {
            freeParticles.add(Particle())
        }
    }
}
