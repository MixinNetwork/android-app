package one.mixin.android.widget.snow

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.animation.AccelerateInterpolator
import android.widget.LinearLayout
import one.mixin.android.extension.dp
import java.util.ArrayList
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin

class SnowLinearLayout(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    private val particlePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
        .apply {
            strokeWidth = 1.5f.dp.toFloat()
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
        }
    private val particleThinPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 0.5f.dp.toFloat()
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.STROKE
    }
    private var lastAnimationTime: Long = 0
    private val angleDiff = (Math.PI / 180 * 60).toFloat()
    private val random = Random()

    private val particles = ArrayList<SnowParticle>()
    private val freeParticles = ArrayList<SnowParticle>()
    private var color = 0
    private val accelerateInterpolator = AccelerateInterpolator()

    init {
        setWillNotDraw(false)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawSnow(canvas)
    }

    private fun updateColors() {
        val color = -0x19191a
        if (this.color != color) {
            this.color = color
            particlePaint.color = color
            particleThinPaint.color = color
        }
    }

    private fun updateParticles(dt: Int) {
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
                    accelerateInterpolator.getInterpolation(particle.currentTime / 200.0f)
            } else {
                particle.alpha =
                    1.0f - accelerateInterpolator.getInterpolation((particle.currentTime - 200.0f) / (particle.lifeTime - 200.0f))
            }
            particle.x += particle.vx * particle.velocity * dt / 500.0f
            particle.y += particle.vy * particle.velocity * dt / 500.0f
            particle.currentTime += dt.toFloat()
            a++
        }
    }

    fun drawSnow(canvas: Canvas) {
        val count = particles.size
        for (a in 0 until count) {
            val particle = particles[a]
            particle.draw(canvas)
        }
        if (random.nextFloat() > 0.7f && particles.size < 100) {
            val cx = random.nextFloat() * measuredWidth
            val cy = random.nextFloat() * measuredHeight
            val angle = random.nextInt(40) - 20 + 90
            val vx = cos(Math.PI / 180.0 * angle).toFloat()
            val vy = sin(Math.PI / 180.0 * angle).toFloat()
            val newParticle: SnowParticle
            if (freeParticles.isNotEmpty()) {
                newParticle = freeParticles[0]
                freeParticles.removeAt(0)
            } else {
                newParticle = SnowParticle(particlePaint, particleThinPaint, angleDiff)
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
        val dt = 17.coerceAtMost((newTime - lastAnimationTime).toInt())
        updateParticles(dt)
        lastAnimationTime = newTime
        invalidate()
    }

    init {
        updateColors()
        repeat(24) {
            freeParticles.add(SnowParticle(particlePaint, particleThinPaint, angleDiff))
        }
    }
}
