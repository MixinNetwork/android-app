package one.mixin.android.ui.wallet.home.components

import android.icu.util.ChineseCalendar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

@Composable
internal fun Modifier.holidaySurpriseEffect(
    timeInMillis: Long = System.currentTimeMillis(),
): Modifier {
    val launchTimeInMillis = remember { timeInMillis }
    val type = remember(launchTimeInMillis) { SurpriseType.from(launchTimeInMillis) } ?: return this
    val effect = remember(type) {
        when (type) {
            SurpriseType.CHRISTMAS -> SnowflakesEffect()
            SurpriseType.CHINESE_NEW_YEAR -> FireworksEffect()
        }
    }
    var frameTimeMillis by remember { mutableStateOf(0L) }

    LaunchedEffect(effect) {
        while (true) {
            withFrameMillis { frameTimeMillis = it }
        }
    }

    return drawBehind {
        if (size.width > 0f && size.height > 0f) {
            effect.draw(this, frameTimeMillis)
        }
    }
}

private enum class SurpriseType {
    CHRISTMAS,
    CHINESE_NEW_YEAR,
    ;

    companion object {
        fun from(timeInMillis: Long): SurpriseType? =
            when {
                isChristmas(timeInMillis) -> CHRISTMAS
                isChineseNewYear(timeInMillis) -> CHINESE_NEW_YEAR
                else -> null
            }

        private fun isChristmas(timeInMillis: Long): Boolean {
            val calendar = Calendar.getInstance().apply {
                this.timeInMillis = timeInMillis
            }
            val year = calendar.get(Calendar.YEAR)
            val christmasBase = Calendar.getInstance().apply {
                set(year, Calendar.DECEMBER, 25, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val start = christmasBase.timeInMillis - MILLIS_PER_DAY
            val end = christmasBase.timeInMillis + MILLIS_PER_DAY * 10
            return timeInMillis in start..end
        }

        private fun isChineseNewYear(timeInMillis: Long): Boolean {
            val current = ChineseCalendar().apply {
                this.timeInMillis = timeInMillis
            }
            val year = current.get(ChineseCalendar.EXTENDED_YEAR)

            return when (current.get(ChineseCalendar.MONTH)) {
                0 -> {
                    val newYearDay = ChineseCalendar().apply {
                        clear()
                        set(ChineseCalendar.EXTENDED_YEAR, year)
                        set(ChineseCalendar.MONTH, 0)
                        set(ChineseCalendar.DAY_OF_MONTH, 1)
                        set(ChineseCalendar.HOUR_OF_DAY, 0)
                        set(ChineseCalendar.MINUTE, 0)
                        set(ChineseCalendar.SECOND, 0)
                        set(ChineseCalendar.MILLISECOND, 0)
                    }
                    val start = newYearDay.timeInMillis
                    val end = start + 6 * MILLIS_PER_DAY
                    timeInMillis in start..end
                }
                11 -> {
                    val nextYearDay = ChineseCalendar().apply {
                        clear()
                        set(ChineseCalendar.EXTENDED_YEAR, year + 1)
                        set(ChineseCalendar.MONTH, 0)
                        set(ChineseCalendar.DAY_OF_MONTH, 1)
                        set(ChineseCalendar.HOUR_OF_DAY, 0)
                        set(ChineseCalendar.MINUTE, 0)
                        set(ChineseCalendar.SECOND, 0)
                        set(ChineseCalendar.MILLISECOND, 0)
                    }
                    val start = nextYearDay.timeInMillis - MILLIS_PER_DAY
                    val end = nextYearDay.timeInMillis + 6 * MILLIS_PER_DAY
                    timeInMillis in start..end
                }
                else -> false
            }
        }
    }
}

private interface HolidayEffect {
    fun draw(
        scope: DrawScope,
        frameTimeMillis: Long,
    )
}

private class SnowflakesEffect : HolidayEffect {
    private val particles = ArrayList<SnowflakeParticle>()
    private val freeParticles = ArrayList<SnowflakeParticle>()
    private val random = Random(System.currentTimeMillis())
    private var lastAnimationTime = 0L

    override fun draw(
        scope: DrawScope,
        frameTimeMillis: Long,
    ) {
        if (random.nextFloat() > 0.7f && particles.size < 100) {
            particles.add(
                obtainParticle().apply {
                    x = random.nextFloat() * scope.size.width
                    y = random.nextFloat() * (scope.size.height - scope.px(20f)).coerceAtLeast(0f)
                    val angle = random.nextInt(40) - 20 + 90
                    vx = cos(PI / 180.0 * angle).toFloat()
                    vy = sin(PI / 180.0 * angle).toFloat()
                    alpha = 0f
                    currentTime = 0f
                    scale = random.nextFloat() * 1.2f
                    type = random.nextInt(2)
                    lifeTime = (2000 + random.nextInt(100)).toFloat()
                    velocity = 20f + random.nextFloat() * 4f
                },
            )
        }

        val dt = frameDelta(frameTimeMillis)
        updateParticles(dt)
        particles.forEach { it.draw(scope) }
    }

    private fun frameDelta(frameTimeMillis: Long): Long {
        val lastTime = lastAnimationTime
        lastAnimationTime = frameTimeMillis
        return if (lastTime == 0L || frameTimeMillis <= lastTime) {
            0L
        } else {
            min(17L, frameTimeMillis - lastTime)
        }
    }

    private fun updateParticles(dt: Long) {
        var index = 0
        while (index < particles.size) {
            val particle = particles[index]
            if (particle.currentTime >= particle.lifeTime) {
                recycleParticle(index, particle)
                continue
            }

            particle.currentTime += dt
            val fadeIn = particle.currentTime / 200f
            val fadeOut = (particle.lifeTime - particle.currentTime) / 200f
            particle.alpha = min(1f, min(fadeIn, fadeOut)).coerceAtLeast(0f)
            particle.x += particle.vx * particle.velocity * dt / 500f
            particle.y += particle.vy * particle.velocity * dt / 500f
            index++
        }
    }

    private fun obtainParticle(): SnowflakeParticle =
        if (freeParticles.isEmpty()) {
            SnowflakeParticle()
        } else {
            freeParticles.removeAt(0)
        }

    private fun recycleParticle(
        index: Int,
        particle: SnowflakeParticle,
    ) {
        particles.removeAt(index)
        if (freeParticles.size < 40) {
            freeParticles.add(particle)
        }
    }
}

private class FireworksEffect : HolidayEffect {
    private val particles = ArrayList<FireworkParticle>()
    private val freeParticles = ArrayList<FireworkParticle>()
    private val random = Random(System.currentTimeMillis())
    private var lastAnimationTime = 0L

    override fun draw(
        scope: DrawScope,
        frameTimeMillis: Long,
    ) {
        val dt = frameDelta(frameTimeMillis)
        updateParticles(dt)
        particles.forEach { it.draw(scope) }

        if (random.nextBoolean() && particles.size < 120) {
            val cx = random.nextFloat() * scope.size.width
            val cy = random.nextFloat() * (scope.size.height - scope.px(16f)).coerceAtLeast(0f)
            val color = FIREWORK_COLORS[random.nextInt(FIREWORK_COLORS.size)]
            repeat(8) {
                val angle = random.nextInt(270) - 225
                particles.add(
                    obtainParticle().apply {
                        x = cx
                        y = cy
                        vx = cos(PI / 180.0 * angle).toFloat() * 1.5f
                        vy = sin(PI / 180.0 * angle).toFloat()
                        this.color = color
                        alpha = 1f
                        currentTime = 0f
                        scale = 1f.coerceAtLeast(random.nextFloat() * 1.5f)
                        lifeTime = (1000 + random.nextInt(1000)).toFloat()
                        velocity = 20f + random.nextFloat() * 4f
                    },
                )
            }
        }
    }

    private fun frameDelta(frameTimeMillis: Long): Long {
        val lastTime = lastAnimationTime
        lastAnimationTime = frameTimeMillis
        return if (lastTime == 0L || frameTimeMillis <= lastTime) {
            0L
        } else {
            min(17L, frameTimeMillis - lastTime)
        }
    }

    private fun updateParticles(dt: Long) {
        var index = 0
        while (index < particles.size) {
            val particle = particles[index]
            if (particle.currentTime >= particle.lifeTime) {
                recycleParticle(index, particle)
                continue
            }

            val progress = (particle.currentTime / particle.lifeTime).coerceIn(0f, 1f)
            particle.alpha = 1f - decelerate(progress)
            particle.x += particle.vx * particle.velocity * dt / 500f
            particle.y += particle.vy * particle.velocity * dt / 500f
            particle.vy += dt / 100f
            particle.currentTime += dt
            index++
        }
    }

    private fun obtainParticle(): FireworkParticle =
        if (freeParticles.isEmpty()) {
            FireworkParticle()
        } else {
            freeParticles.removeAt(0)
        }

    private fun recycleParticle(
        index: Int,
        particle: FireworkParticle,
    ) {
        particles.removeAt(index)
        if (freeParticles.size < 40) {
            freeParticles.add(particle)
        }
    }

    private fun decelerate(progress: Float): Float {
        val inverse = 1f - progress
        return 1f - inverse * inverse
    }
}

private class SnowflakeParticle {
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

    fun draw(scope: DrawScope) {
        if (type == 0) {
            scope.drawCircle(
                color = SNOW_COLOR.copy(alpha = alpha),
                radius = scope.px(0.75f) * scale.coerceAtLeast(0.4f),
                center = Offset(x, y),
            )
        } else {
            drawSnowflake(scope)
        }
    }

    private fun drawSnowflake(scope: DrawScope) {
        val strokeWidth = scope.px(0.5f)
        val armLength = scope.px(4f) * scale.coerceAtLeast(0.4f)
        val branchLength = scope.px(1.15f) * scale.coerceAtLeast(0.4f)
        val branchOffset = scope.px(2.3f) * scale.coerceAtLeast(0.4f)
        val color = SNOW_COLOR.copy(alpha = alpha)

        repeat(6) { index ->
            val angle = (-PI / 2 + index * PI / 3).toFloat()
            val end = Offset(
                x = x + cos(angle) * armLength,
                y = y + sin(angle) * armLength,
            )
            val branchCenter = Offset(
                x = x + cos(angle) * branchOffset,
                y = y + sin(angle) * branchOffset,
            )
            scope.drawLine(
                color = color,
                start = Offset(x, y),
                end = end,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            scope.drawLine(
                color = color,
                start = branchCenter,
                end = Offset(
                    x = branchCenter.x + cos(angle - PI.toFloat() / 4) * branchLength,
                    y = branchCenter.y + sin(angle - PI.toFloat() / 4) * branchLength,
                ),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            scope.drawLine(
                color = color,
                start = branchCenter,
                end = Offset(
                    x = branchCenter.x + cos(angle + PI.toFloat() / 4) * branchLength,
                    y = branchCenter.y + sin(angle + PI.toFloat() / 4) * branchLength,
                ),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

private class FireworkParticle {
    var x = 0f
    var y = 0f
    var vx = 0f
    var vy = 0f
    var velocity = 0f
    var alpha = 0f
    var lifeTime = 0f
    var currentTime = 0f
    var scale = 0f
    var color = Color.Unspecified

    fun draw(scope: DrawScope) {
        scope.drawCircle(
            color = color.copy(alpha = alpha.coerceIn(0f, 1f)),
            radius = scope.px(0.75f) * scale.coerceAtLeast(0.8f),
            center = Offset(x, y),
        )
    }
}

private val SNOW_COLOR = Color(0xFFE6E6E6)
private val FIREWORK_COLORS = listOf(
    Color(0xFFFD141D),
    Color(0xFFFF8729),
    Color(0xFF0096DF),
    Color(0xFFC530F6),
    Color(0xFF29F336),
    Color(0xFFF2CA36),
)

private const val MILLIS_PER_DAY = 86_400_000L

private fun DrawScope.px(value: Float): Float = value.dp.toPx()
