package one.mixin.android.inscription.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import com.walletconnect.util.bytesToHex
import one.mixin.android.crypto.sha3Sum256
import one.mixin.android.extension.hexStringToByteArray
import kotlin.math.max

@Composable
fun Barcode(hash: String, modifier: Modifier) {
    val colors = buildColors(hash)
    Box(modifier = modifier) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val itemWith = LocalDensity.current.run { maxWidth.toPx() } / 128
            val width = itemWith * 7
            val offset = itemWith * 4
            val radius = max(width / 4f, 2f)
            Canvas(modifier = Modifier.fillMaxSize()) {
                colors.forEachIndexed { index, color ->
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x = (width + offset) * index, y = 0f),
                        size = Size(width, size.height),
                        cornerRadius = CornerRadius(radius, radius)
                    )
                }
            }
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
