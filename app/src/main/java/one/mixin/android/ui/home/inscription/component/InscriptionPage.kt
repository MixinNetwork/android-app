package one.mixin.android.ui.home.inscription.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.ShimmerTheme
import com.valentinilk.shimmer.rememberShimmer
import com.valentinilk.shimmer.shimmer
import com.valentinilk.shimmer.shimmerSpec
import one.mixin.android.R
import one.mixin.android.compose.GlideImage
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.ui.setting.LocalSettingNav
import one.mixin.android.inscription.compose.Barcode
import one.mixin.android.ui.home.web3.components.InscriptionState

@Composable
fun InscriptionPage(inscriptionHash: String, onSendAction: () -> Unit, onShareAction: () -> Unit) {
    val scrollState = rememberScrollState()
    val viewModel = hiltViewModel<Web3ViewModel>()
    val inscriptionItem = viewModel.inscriptionByHash(inscriptionHash).observeAsState().value ?: return
    val shimmerInstance = rememberShimmer(
        shimmerBounds = ShimmerBounds.View, theme = ShimmerTheme(
            animationSpec = infiniteRepeatable(
                animation = shimmerSpec(
                    durationMillis = 1_500,
                    easing = LinearEasing,
                    delayMillis = 1_500,
                ),
                repeatMode = RepeatMode.Restart,
            ),
            blendMode = BlendMode.DstIn,
            rotation = 15.0f,
            shaderColors = listOf(
                Color.Unspecified.copy(alpha = 1.0f),
                Color.Unspecified.copy(alpha = 0.7f),
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

    val contentUrl = inscriptionItem.contentURL
    val iconUrl = inscriptionItem.iconURL
    val idTitle = "#${inscriptionItem.sequence}"

    val state = remember {
        mutableStateOf(InscriptionState("","","",""))
    }

    LaunchedEffect(key1 = inscriptionItem) {
        val result = viewModel.loadData(inscriptionHash) ?: return@LaunchedEffect
        state.value = result
    }
    Box(Modifier.background(Color(0xFF000000))) {
        AsyncImage(
            model = contentUrl, contentDescription = null,
            modifier = Modifier
                .fillMaxHeight()
                .graphicsLayer {
                    alpha = 0.5f
                }
                .blur(30.dp),
            placeholder = painterResource(R.drawable.ic_default_inscription),
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
                        data = contentUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .shimmer(shimmerInstance)
                            .clip(RoundedCornerShape(8.dp)),
                        placeHolderPainter = painterResource(id = R.drawable.ic_default_inscription),
                    )
                }
                Box(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.padding(horizontal = 12.dp)) {
                    if (state.value.state == "unspent") {
                        Button(
                            onClick = onSendAction, colors = ButtonDefaults.outlinedButtonColors(
                                backgroundColor = Color(0xFF, 0xFF, 0xFF, 0x1F)
                            ), modifier = Modifier.weight(1f), shape = RoundedCornerShape(20.dp), contentPadding = PaddingValues(vertical = 12.dp), elevation = ButtonDefaults.elevation(
                                pressedElevation = 0.dp, defaultElevation = 0.dp, hoveredElevation = 0.dp, focusedElevation = 0.dp
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center
                            ) {
                                Text(text = stringResource(id = R.string.Send), color = Color.White)
                            }
                        }

                        Box(modifier = Modifier.width(28.dp))
                    }
                    Button(
                        onClick = {
                            onShareAction.invoke()
                        }, colors = ButtonDefaults.outlinedButtonColors(
                            backgroundColor = Color(0xFF, 0xFF, 0xFF, 0x1F)
                        ), modifier = Modifier.weight(1f), shape = RoundedCornerShape(20.dp), contentPadding = PaddingValues(vertical = 11.dp), elevation = ButtonDefaults.elevation(
                            pressedElevation = 0.dp, defaultElevation = 0.dp, hoveredElevation = 0.dp, focusedElevation = 0.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = stringResource(id = R.string.Share), color = Color.White)
                        }
                    }

                }

                Box(modifier = Modifier.height(28.dp))

                Text(text = stringResource(id = R.string.HASH), fontSize = 16.sp, color = Color(0xFF999999))
                Box(modifier = Modifier.height(8.dp))
                Barcode(
                    inscriptionHash, modifier = Modifier
                        .width(128.dp)
                        .height(24.dp)
                )
                Box(modifier = Modifier.height(4.dp))
                Text(text = inscriptionHash, fontSize = 16.sp, color = Color(0xFF999999))

                Box(modifier = Modifier.height(20.dp))
                Text(text = stringResource(id = R.string.ID), fontSize = 16.sp, color = Color(0xFF999999))
                Box(modifier = Modifier.height(8.dp))
                Text(text = idTitle, fontSize = 16.sp, color = Color.White)

                Box(modifier = Modifier.height(20.dp))
                Text(text = stringResource(id = R.string.COLLECTION), fontSize = 16.sp, color = Color(0xFF999999))
                Box(modifier = Modifier.height(8.dp))
                Text(text = state.value.collection, fontSize = 16.sp, color = Color.White)


                Box(modifier = Modifier.height(20.dp))
                Box(Modifier.fillMaxWidth()) {
                    Column {
                        Text(text = stringResource(id = R.string.NFT_TOKEN), fontSize = 16.sp, color = Color(0xFF999999))
                        Box(modifier = Modifier.height(8.dp))
                        Text(text = state.value.tokenTotal, fontSize = 16.sp, color = Color.White)
                        Box(modifier = Modifier.height(5.dp))
                        Text(text = state.value.tokenValue, fontSize = 14.sp, color = Color(0xFF999999))
                    }

                    AsyncImage(
                        model = iconUrl, contentDescription = null,
                        modifier = Modifier.align(Alignment.CenterEnd).width(20.dp).height(20.dp).clip(RoundedCornerShape(4.dp)),
                        placeholder = painterResource(R.drawable.ic_inscription_icon),
                    )
                }


                Box(modifier = Modifier.height(70.dp))
            }
        }
    }
}