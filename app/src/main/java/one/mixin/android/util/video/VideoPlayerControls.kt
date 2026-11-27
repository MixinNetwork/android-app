package one.mixin.android.util.video

import androidx.media3.common.C

import kotlin.math.abs

internal object VideoPlayerControls {
    private val speeds = floatArrayOf(1f, 1.25f, 1.5f, 2f)

    fun nextSpeed(current: Float): Float {
        val index = speeds.indexOfFirst { abs(it - current) < 0.01f }.coerceAtLeast(0)
        return speeds[(index + 1) % speeds.size]
    }

    fun seekTarget(
        positionMs: Long,
        deltaMs: Long,
        durationMs: Long,
    ): Long {
        val end = if (durationMs == C.TIME_UNSET) Long.MAX_VALUE else durationMs.coerceAtLeast(0L)
        val position = positionMs.coerceAtLeast(0L)
        return position + deltaMs.coerceIn(-position, end - position)
    }

    fun frameBucket(positionMs: Long): Long = (positionMs / FRAME_BUCKET_MS) * FRAME_BUCKET_MS

    private const val FRAME_BUCKET_MS = 500L
}
