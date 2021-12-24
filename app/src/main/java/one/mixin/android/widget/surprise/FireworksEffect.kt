package one.mixin.android.widget.surprise

import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import android.view.animation.DecelerateInterpolator
import one.mixin.android.extension.dp
import java.util.ArrayList
import java.util.Random
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class FireworksEffect {
    private val particlePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var lastAnimationTime: Long = 0
    private val random = Random()

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
        var color = 0
        var type = 0
        fun draw(canvas: Canvas) {
            when (type) {
                0 -> {
                    particlePaint.color = color
                    particlePaint.strokeWidth = 1.5f.dp * scale
                    particlePaint.alpha = (255 * alpha).toInt()
                    canvas.drawPoint(x, y, particlePaint)
                }
                1 -> {}
            }
        }
    }

    private val particles = ArrayList<Particle>()
    private val freeParticles = ArrayList<Particle>()
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
            particle.alpha =
                1.0f - DecelerateInterpolator().getInterpolation(particle.currentTime / particle.lifeTime)
            particle.x += particle.vx * particle.velocity * dt / 500.0f
            particle.y += particle.vy * particle.velocity * dt / 500.0f
            particle.vy += dt / 100.0f
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
        if (Random().nextBoolean() && particles.size + 8 < 150) {
            val cx = random.nextFloat() * parent.measuredWidth
            val cy = random.nextFloat() * (parent.measuredHeight - 16f.dp)
            val color: Int = when (random.nextInt(4)) {
                0 -> {
                    0XFD141D
                }
                1 -> {
                    0XFF8729
                }
                2 -> {
                    0X0096DF
                }
                3 -> {
                    0XC530F6
                }
                4 -> {
                    0X29F336
                }
                else -> {
                    0XF2CA36
                }
            }
            for (a in 0..7) {
                val angle = random.nextInt(270) - 225
                val vx = cos(PI / 180.0 * angle).toFloat()
                val vy = sin(PI / 180.0 * angle).toFloat()
                var newParticle: Particle
                if (freeParticles.isNotEmpty()) {
                    newParticle = freeParticles[0]
                    freeParticles.removeAt(0)
                } else {
                    newParticle = Particle()
                }
                newParticle.x = cx
                newParticle.y = cy
                newParticle.vx = vx * 1.5f
                newParticle.vy = vy
                newParticle.color = color
                newParticle.alpha = 1.0f
                newParticle.currentTime = 0f
                newParticle.scale = 1.0f.coerceAtLeast(random.nextFloat() * 1.5f)
                newParticle.type = 0
                newParticle.lifeTime = (1000 + random.nextInt(1000)).toFloat()
                newParticle.velocity = 20.0f + random.nextFloat() * 4.0f
                particles.add(newParticle)
            }
        }
        val newTime = System.currentTimeMillis()
        val dt = min(17, newTime - lastAnimationTime)
        updateParticles(dt)
        lastAnimationTime = newTime
        parent.invalidate()
    }

    init {
        particlePaint.strokeWidth = 1.5f.dp.toFloat()
        particlePaint.color = -0x19191a
        particlePaint.strokeCap = Paint.Cap.ROUND
        particlePaint.style = Paint.Style.STROKE
        for (a in 0..19) {
            freeParticles.add(Particle())
        }
    }
}
