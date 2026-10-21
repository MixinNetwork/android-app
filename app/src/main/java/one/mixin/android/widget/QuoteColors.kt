package one.mixin.android.widget

import android.content.Context
import android.graphics.Color
import android.graphics.ColorSpace
import android.os.Build
import android.text.TextPaint
import android.text.style.CharacterStyle
import one.mixin.android.extension.isScreenWideColorGamut

/**
 * Rise/fall green #00B86B and red #F55666.
 * On a wide-gamut screen the same channel values are used as Display P3, which is more saturated than the sRGB hex.
 */
object QuoteColors {
    data class Channels(
        val r: Float,
        val g: Float,
        val b: Float,
        val srgb: Long,
        val rgb: Int,
    )

    val Green = Channels(0f, 0.722f, 0.420f, 0xFF00B86B, 0x00B86B)
    val Red = Channels(0.961f, 0.337f, 0.400f, 0xFFF55666, 0xF55666)

    private val displayP3: ColorSpace by lazy {
        ColorSpace.get(ColorSpace.Named.DISPLAY_P3)
    }

    fun supportsWideGamut(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= 29 && context.isScreenWideColorGamut()
    }

    fun colorLongOrNull(color: Int): Long? {
        if (Build.VERSION.SDK_INT < 29) return null
        val channels = channelsFor(color) ?: return null
        val alpha = ((color ushr 24) and 0xFF) / 255f
        return Color.pack(channels.r, channels.g, channels.b, alpha, displayP3)
    }

    private fun channelsFor(color: Int): Channels? {
        return when (color and 0xFFFFFF) {
            Green.rgb -> Green
            Red.rgb -> Red
            else -> null
        }
    }
}

class WideGamutColorSpan(
    val packedColor: Long,
    private val expectedRgb: Int,
    val upgrade: Boolean,
) : CharacterStyle() {
    override fun updateDrawState(tp: TextPaint) {
        if (Build.VERSION.SDK_INT < 29) return
        if (upgrade && (tp.color and 0xFFFFFF) != expectedRgb) return
        tp.setColor(packedColor)
    }
}
