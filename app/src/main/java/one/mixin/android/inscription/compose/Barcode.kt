package one.mixin.android.inscription.compose

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.reown.util.bytesToHex
import one.mixin.android.crypto.sha3Sum256
import one.mixin.android.extension.hexStringToByteArray
import kotlin.math.max

@Composable
fun Barcode(
    hash: String,
    modifier: Modifier,
) {
    val colors = remember(hash) { buildColors(hash) }
    Canvas(modifier = modifier) {
        val itemWidth = size.width / 128f
        val width = itemWidth * 7f
        val offset = itemWidth * 4f
        val radius = max(width / 4f, 2f)
        colors.forEachIndexed { index, color ->
            drawRoundRect(
                color = color,
                topLeft = Offset(x = (width + offset) * index, y = 0f),
                size = Size(width, size.height),
                cornerRadius = CornerRadius(radius, radius),
            )
        }
    }
}

private fun buildColors(hash: String): MutableList<Color> {
    val colors = mutableListOf<Color>()
    val bytes = hash.hexStringToByteArray()
    val data = hash + bytes.sha3Sum256().slice(IntRange(0, 3)).toByteArray().bytesToHex()
    colors.clear()
    data.chunked(6).forEach { colorString ->
        val color = (colorString.toLong(16) or 0x00000000ff000000L).toInt()
        colors.add(Color(color))
    }
    return colors
}
