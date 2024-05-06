package one.mixin.android.web3.compose


import one.mixin.android.compose.GlideImage
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemGesturesPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.valentinilk.shimmer.Shimmer
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.ShimmerTheme
import com.valentinilk.shimmer.rememberShimmer
import com.valentinilk.shimmer.shimmer
import com.valentinilk.shimmer.shimmerSpec
import com.walletconnect.util.bytesToHex
import com.walletconnect.util.randomBytes
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.crypto.sha3Sum256
import one.mixin.android.extension.hexString
import one.mixin.android.extension.hexStringToByteArray
import one.mixin.android.extension.inTransaction
import one.mixin.android.ui.conversation.FriendsFragment
import one.mixin.android.ui.setting.LocalSettingNav
import one.mixin.android.util.SystemUIManager

@Composable
fun Barcode(hash: String, modifier: Modifier) {
    val colors = buildColors(hash)
    Box(modifier = modifier) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val itemWith = LocalDensity.current.run { maxWidth.toPx() } / 128
            val width = itemWith * 7
            val offset = itemWith * 4
            Canvas(modifier = Modifier.fillMaxSize()) {
                colors.forEachIndexed { index, color ->
                    drawRect(
                        color = color,
                        topLeft = Offset(x = (width + offset) * index, y = 0f),
                        size = Size(width, size.height)
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
