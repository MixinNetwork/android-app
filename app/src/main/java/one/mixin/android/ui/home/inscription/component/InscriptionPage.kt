package one.mixin.android.ui.home.inscription.component

import android.annotation.SuppressLint
import android.os.Build
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.request.ImageRequest
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.inscription.compose.Barcode
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.ui.home.web3.components.InscriptionState
import one.mixin.android.widget.BlurTransformation
import one.mixin.android.widget.CoilRoundedHexagonTransformation

@SuppressLint("UnrememberedMutableState")
@Composable
fun InscriptionPage(
    inscriptionHash: String,
    onCloseAction: () -> Unit,
    onMoreAction: () -> Unit,
    onSendAction: () -> Unit,
    onShareAction: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel = hiltViewModel<Web3ViewModel>()
    val liveData = viewModel.inscriptionStateByHash(inscriptionHash)
    val inscription =
        remember {
            mutableStateOf<InscriptionState?>(null)
        }
    DisposableEffect(inscriptionHash, lifecycleOwner) {
        val observer =
            Observer<InscriptionState?> {
                inscription.value = it
            }
        liveData.observe(lifecycleOwner, observer)
        onDispose { liveData.removeObserver(observer) }
    }
    val value = inscription.value
    if (value == null) {
        Box {}
    } else {
        InscriptionPageImp(value, inscriptionHash, onCloseAction, onMoreAction, onSendAction, onShareAction)
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun InscriptionPageImp(
    inscription: InscriptionState,
    inscriptionHash: String,
    onCloseAction: () -> Unit,
    onMoreAction: () -> Unit,
    onSendAction: () -> Unit,
    onShareAction: () -> Unit,
) {
    val scrollState = rememberScrollState()
    var expend by remember {
        mutableStateOf(false)
    }
    Box(
        Modifier
            .background(Color(0xFF000000))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                expend = false
            },
    ) {
        BlurImage(inscription.contentURL)

        Column(
            modifier =
                Modifier
                    .statusBarsPadding()
                    .navigationBarsPadding(),
        ) {
            Row {
                // Align to image
                Spacer(modifier = Modifier.width(4.dp))
                if (expend) {
                    IconButton(onClick = {
                        expend = false
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = null,
                            tint = Color.White,
                        )
                    }
                } else {
                    IconButton(onClick = onCloseAction) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = null,
                            tint = Color.White,
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onMoreAction) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_home_more),
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
            }
            SharedTransitionLayout {
                AnimatedContent(
                    expend,
                    label = "basic_transition",
                ) { targetState ->
                    if (!targetState) {
                        with(this@AnimatedContent) {
                            Column(
                                modifier =
                                    Modifier
                                        .verticalScroll(scrollState)
                                        .padding(horizontal = 20.dp)
                                        .fillMaxSize(),
                            ) {
                                Box(modifier = Modifier.height(20.dp))

                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f),
                                ) {
                                    CoilImage(
                                        model = inscription.contentURL,
                                        modifier =
                                            Modifier
                                                .sharedElement(
                                                    rememberSharedContentState(key = "image"),
                                                    animatedVisibilityScope = this@AnimatedContent,
                                                )
                                                .fillMaxWidth()
                                                .fillMaxHeight()
                                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                                    expend = !expend
                                                }
                                                .clip(RoundedCornerShape(8.dp)),
                                        placeholder = R.drawable.ic_inscription_content,
                                    )
                                }
                                if (!expend) {
                                    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                                        Box(modifier = Modifier.height(20.dp))

                                        Row(modifier = Modifier.padding(horizontal = 4.dp)) {
                                            if (inscription.state == "unspent") {
                                                Button(
                                                    onClick = onSendAction,
                                                    colors =
                                                        ButtonDefaults.outlinedButtonColors(
                                                            backgroundColor = Color(0xFF, 0xFF, 0xFF, 0x1F),
                                                        ),
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(20.dp),
                                                    contentPadding = PaddingValues(vertical = 12.dp),
                                                    elevation =
                                                        ButtonDefaults.elevation(
                                                            pressedElevation = 0.dp,
                                                            defaultElevation = 0.dp,
                                                            hoveredElevation = 0.dp,
                                                            focusedElevation = 0.dp,
                                                        ),
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.Center,
                                                    ) {
                                                        Text(text = stringResource(id = R.string.Send), color = Color.White)
                                                    }
                                                }

                                                Box(modifier = Modifier.width(28.dp))
                                            }
                                            Button(
                                                onClick = {
                                                    onShareAction.invoke()
                                                },
                                                colors =
                                                    ButtonDefaults.outlinedButtonColors(
                                                        backgroundColor = Color(0xFF, 0xFF, 0xFF, 0x1F),
                                                    ),
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(20.dp),
                                                contentPadding = PaddingValues(vertical = 11.dp),
                                                elevation =
                                                    ButtonDefaults.elevation(
                                                        pressedElevation = 0.dp,
                                                        defaultElevation = 0.dp,
                                                        hoveredElevation = 0.dp,
                                                        focusedElevation = 0.dp,
                                                    ),
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.Center,
                                                ) {
                                                    Text(text = stringResource(id = R.string.Share), color = Color.White)
                                                }
                                            }
                                        }

                                        Box(modifier = Modifier.height(28.dp))

                                        Text(text = stringResource(id = R.string.HASH), fontSize = 16.sp, color = Color(0xFF999999))
                                        Box(modifier = Modifier.height(8.dp))
                                        Barcode(
                                            inscriptionHash,
                                            modifier =
                                                Modifier
                                                    .width(128.dp)
                                                    .height(24.dp),
                                        )
                                        Box(modifier = Modifier.height(4.dp))
                                        SelectionContainer {
                                            Text(text = inscriptionHash, fontSize = 16.sp, color = Color(0xFF999999))
                                        }

                                        Box(modifier = Modifier.height(20.dp))
                                        Text(text = stringResource(id = R.string.ID), fontSize = 16.sp, color = Color(0xFF999999))
                                        Box(modifier = Modifier.height(8.dp))
                                        Text(text = inscription.id, fontSize = 16.sp, color = Color.White)

                                        Box(modifier = Modifier.height(20.dp))
                                        Text(text = stringResource(id = R.string.Collection).uppercase(), fontSize = 16.sp, color = Color(0xFF999999))
                                        Box(modifier = Modifier.height(8.dp))
                                        Text(text = inscription.name ?: "", fontSize = 16.sp, color = Color.White)

                                        Box(modifier = Modifier.height(20.dp))
                                        Box(Modifier.fillMaxWidth()) {
                                            Column {
                                                Text(text = stringResource(id = R.string.NFT_TOKEN), fontSize = 16.sp, color = Color(0xFF999999))
                                                Box(modifier = Modifier.height(8.dp))
                                                Text(text = inscription.tokenTotal, fontSize = 16.sp, color = Color.White)
                                                Box(modifier = Modifier.height(5.dp))
                                                Text(text = inscription.valueAs, fontSize = 14.sp, color = Color(0xFF999999))
                                            }

                                            CoilImage(
                                                model =
                                                    ImageRequest.Builder(LocalContext.current)
                                                        .data(inscription.iconUrl)
                                                        .transformations(CoilRoundedHexagonTransformation())
                                                        .build(),
                                                modifier =
                                                    Modifier
                                                        .align(Alignment.CenterEnd)
                                                        .width(20.dp)
                                                        .height(20.dp)
                                                        .clip(RoundedCornerShape(4.dp)),
                                                placeholder = R.drawable.ic_inscription_icon,
                                            )
                                        }

                                        Box(modifier = Modifier.height(70.dp))
                                    }
                                }
                            }
                        }
                    } else {
                        val callback =
                            remember {
                                object : OnBackPressedCallback(true) {
                                    override fun handleOnBackPressed() {
                                        expend = !expend
                                    }
                                }
                            }
                        val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
                        DisposableEffect(key1 = this@AnimatedContent) {
                            dispatcher?.addCallback(callback)
                            onDispose {
                                callback.remove()
                            }
                        }
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .fillMaxHeight(),
                        ) {
                            with(this@AnimatedContent) {
                                CoilImage(
                                    model = inscription.contentURL,
                                    modifier =
                                        Modifier
                                            .align(Alignment.Center)
                                            .sharedElement(
                                                rememberSharedContentState(key = "image"),
                                                animatedVisibilityScope = this@AnimatedContent,
                                            )
                                            .fillMaxWidth()
                                            .fillMaxHeight()
                                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                                expend = !expend
                                            },
                                    placeholder = R.drawable.ic_inscription_content,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlurImage(url: String?) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        CoilImage(
            model = url,
            modifier =
                Modifier
                    .fillMaxHeight()
                    .graphicsLayer {
                        alpha = 0.5f
                    }
                    .blur(30.dp),
            placeholder = R.drawable.ic_inscription_content,
            contentScale = ContentScale.Crop,
        )
    } else {
        CoilImage(
            model =
                ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .transformations(BlurTransformation(LocalContext.current))
                    .build(),
            modifier =
                Modifier
                    .fillMaxHeight(),
            placeholder = R.drawable.ic_inscription_content,
            contentScale = ContentScale.Crop,
        )
    }
}
