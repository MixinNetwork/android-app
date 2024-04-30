package one.mixin.android.ui.home.web3.inscription

import GlideImage
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
import one.mixin.android.ui.setting.LocalSettingNav
import one.mixin.android.util.SystemUIManager

@AndroidEntryPoint
class InscriptionActivity : AppCompatActivity() {
    companion object {
        fun show(context: Context) {
            Intent(context, InscriptionActivity::class.java).run {
                context.startActivity(this)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SystemUIManager.lightUI(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        // Todo
        val iconUrl = ""
        val hash = randomBytes(32).hexString()
        val avatarUrl = ""
        val idTitle = ""
        val collection = ""
        val tokenTotal = ""
        val tokenValue= ""
        val userName = ""
        val rarity = ""

        setContent {
            val scrollState = rememberScrollState()
            val shimmerInstance = rememberShimmer(
                shimmerBounds = ShimmerBounds.View, theme = ShimmerTheme(
                    animationSpec = infiniteRepeatable(
                        animation = shimmerSpec(
                            durationMillis = 800,
                            easing = LinearEasing,
                            delayMillis = 1_500,
                        ),
                        repeatMode = RepeatMode.Restart,
                    ),
                    blendMode = BlendMode.DstIn,
                    rotation = 15.0f,
                    shaderColors = listOf(
                        Color.Unspecified.copy(alpha = 1.0f),
                        Color.Unspecified.copy(alpha = 0.75f),
                        Color.Unspecified.copy(alpha = 1.0f),
                    ),
                    shaderColorStops = listOf(
                        0.0f,
                        0.5f,
                        1.0f,
                    ),
                    shimmerWidth = 400.dp,
                )
            )

            Box(Modifier.background(Color(0xFF000000))) {
                GlideImage(
                    data = iconUrl,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(30.dp)
                        .graphicsLayer {
                            alpha = 0.5f
                        }
                        .shimmer(shimmerInstance),
                    placeHolderPainter = painterResource(id = R.drawable.ic_default_inscription),
                    contentScale = ContentScale.Crop
                )
                Column(
                    modifier = Modifier.systemGesturesPadding()
                ) {
                    val navController = LocalSettingNav.current
                    IconButton(onClick = { navController.pop() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = null,
                            tint = Color.White,
                        )
                    }
                    Column(
                        modifier = Modifier
                            .verticalScroll(scrollState)
                            .padding(horizontal = 20.dp)
                            .fillMaxSize()
                    ) {
                        Box(modifier = Modifier.height(20.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                        ) {
                            GlideImage(
                                data = iconUrl,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp)),
                                placeHolderPainter = painterResource(id = R.drawable.ic_default_inscription),
                            )
                        }
                        Box(modifier = Modifier.height(20.dp))

                        Row(modifier = Modifier.padding(horizontal = 12.dp)) {
                            Button(
                                onClick = {},
                                colors =
                                ButtonDefaults.outlinedButtonColors(
                                    backgroundColor = Color(0xFF, 0xFF, 0xFF, 0x1F)
                                ),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues( vertical = 12.dp),
                                elevation = ButtonDefaults.elevation(
                                    pressedElevation = 0.dp,
                                    defaultElevation = 0.dp,
                                    hoveredElevation = 0.dp,
                                    focusedElevation = 0.dp
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(text = stringResource(id = R.string.Send), color = Color.White)
                                }
                            }

                            Box(modifier = Modifier.width(28.dp))

                            Button(
                                onClick = {},
                                colors =
                                ButtonDefaults.outlinedButtonColors(
                                    backgroundColor = Color(0xFF, 0xFF, 0xFF, 0x1F)
                                ),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(vertical = 11.dp),
                                elevation = ButtonDefaults.elevation(
                                    pressedElevation = 0.dp,
                                    defaultElevation = 0.dp,
                                    hoveredElevation = 0.dp,
                                    focusedElevation = 0.dp
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(text = stringResource(id = R.string.Share), color = Color.White)
                                }
                            }

                        }

                        Box(modifier = Modifier.height(28.dp))

                        Text(text = stringResource(id = R.string.HASH), fontSize = 16.sp, color = Color(0xFF999999))
                        Box(modifier = Modifier.height(8.dp))
                        Barcode(
                            hash, modifier =
                            Modifier
                                .width(128.dp)
                                .height(24.dp)
                        )
                        Box(modifier = Modifier.height(4.dp))
                        Text(text = hash, fontSize = 16.sp, color = Color(0xFF999999))

                        Box(modifier = Modifier.height(20.dp))
                        Text(text = stringResource(id = R.string.ID), fontSize = 16.sp, color = Color(0xFF999999))
                        Box(modifier = Modifier.height(8.dp))
                        Text(text = idTitle, fontSize = 16.sp, color = Color.White)

                        Box(modifier = Modifier.height(20.dp))
                        Text(text = stringResource(id = R.string.COLLECTION), fontSize = 16.sp, color = Color(0xFF999999))
                        Box(modifier = Modifier.height(8.dp))
                        Text(text = collection, fontSize = 16.sp, color = Color.White)


                        Box(modifier = Modifier.height(20.dp))
                        Text(text = stringResource(id = R.string.NFT_TOKEN), fontSize = 16.sp, color = Color(0xFF999999))
                        Box(modifier = Modifier.height(8.dp))
                        Text(text = tokenTotal, fontSize = 16.sp, color = Color.White)
                        Box(modifier = Modifier.height(5.dp))
                        Text(text = tokenValue, fontSize = 14.sp, color = Color(0xFF999999))

                        Box(modifier = Modifier.height(20.dp))
                        Text(text = stringResource(id = R.string.CREATOR), fontSize = 16.sp, color = Color(0xFF999999))
                        Box(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically){
                            GlideImage(
                                data = avatarUrl,
                                modifier =
                                Modifier
                                    .width(18.dp)
                                    .height(18.dp)
                                    .clip(CircleShape),
                                placeHolderPainter = painterResource(id = R.drawable.ic_avatar_place_holder),
                            )
                            Box(modifier = Modifier.width(4.dp))
                            Text(text = userName, fontSize = 16.sp, color = Color.White)
                        }


                        Box(modifier = Modifier.height(20.dp))
                        Text(text = stringResource(id = R.string.RARITY), fontSize = 16.sp, color = Color(0xFF999999))
                        Box(modifier = Modifier.height(8.dp))
                        Text(text = rarity, fontSize = 16.sp, color = Color.White)

                        Box(modifier = Modifier.height(70.dp))
                    }
                }
            }
        }
    }
}

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

fun buildColors(hash: String): MutableList<Color> {
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

