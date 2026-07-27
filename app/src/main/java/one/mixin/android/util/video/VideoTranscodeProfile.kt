package one.mixin.android.util.video

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class VideoTranscodeProfile(
    val width: Int,
    val height: Int,
    val bitrate: Int,
) {
    fun needsResize(
        originalWidth: Int,
        originalHeight: Int,
    ): Boolean = width != originalWidth || height != originalHeight
}

object VideoTranscodeProfileFactory {
    private const val MAX_VIDEO_SIZE = 1280f
    private const val DEFAULT_VIDEO_BITRATE = 921_600

    fun create(
        originalWidth: Int,
        originalHeight: Int,
        originalBitrate: Int,
    ): VideoTranscodeProfile {
        require(originalWidth > 0 && originalHeight > 0)

        val scale = min(1f, MAX_VIDEO_SIZE / max(originalWidth, originalHeight))
        val width = (originalWidth * scale / 2).roundToInt() * 2
        val height = (originalHeight * scale / 2).roundToInt() * 2
        val bitrate =
            when {
                originalBitrate <= 0 -> DEFAULT_VIDEO_BITRATE
                scale < 1f -> makeVideoBitrate(originalHeight, originalWidth, originalBitrate, height, width)
                else -> originalBitrate
            }
        return VideoTranscodeProfile(width, height, bitrate)
    }

    internal fun makeVideoBitrate(
        originalHeight: Int,
        originalWidth: Int,
        originalBitrate: Int,
        height: Int,
        width: Int,
    ): Int {
        val minDimension = min(height, width)
        val maxBitrate: Int
        val compressFactor: Float
        val minCompressFactor: Float
        when {
            minDimension >= 1080 -> {
                maxBitrate = 6_800_000
                compressFactor = 1f
                minCompressFactor = 1f
            }
            minDimension >= 720 -> {
                maxBitrate = 2_600_000
                compressFactor = 1f
                minCompressFactor = 1f
            }
            minDimension >= 480 -> {
                maxBitrate = 1_000_000
                compressFactor = 0.75f
                minCompressFactor = 0.9f
            }
            else -> {
                maxBitrate = 750_000
                compressFactor = 0.6f
                minCompressFactor = 0.7f
            }
        }
        val scale = min(originalHeight.toFloat() / height, originalWidth.toFloat() / width)
        val remeasuredBitrate = (originalBitrate / scale * compressFactor).toInt()
        val minBitrate =
            (videoBitrateWithFactor(minCompressFactor) / (1280f * 720f / (width * height))).toInt()
        if (originalBitrate < minBitrate) {
            return remeasuredBitrate
        }
        return min(maxBitrate, max(remeasuredBitrate, minBitrate))
    }

    private fun videoBitrateWithFactor(factor: Float): Int = (factor * 2_000_000f * 1.13f).toInt()
}
