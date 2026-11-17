package one.mixin.android.ui.common

import kotlin.math.roundToInt

internal const val SHARE_IMAGE_SIZE = 1024

internal fun shareImageSize(width: Int, height: Int): Pair<Int, Int> {
    require(width > 0 && height > 0)
    val scale = SHARE_IMAGE_SIZE.toDouble() / maxOf(width, height)
    return (width * scale).roundToInt().coerceAtLeast(1) to
        (height * scale).roundToInt().coerceAtLeast(1)
}
