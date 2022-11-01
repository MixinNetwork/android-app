package one.mixin.android.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.extension.pxToDp
import one.mixin.android.extension.tickVibrate
import one.mixin.android.ui.setting.ui.theme.MixinAppTheme

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PinKeyBoard(
    callback: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var size by remember { mutableStateOf(IntSize.Zero) }
    var loading by remember { mutableStateOf(false) }
    var pinCode by remember { mutableStateOf("") }
    val list = listOf(
        "1", "2", "3",
        "4", "5", "6",
        "7", "8", "9",
        "", "0", "<"
    )
    Column(
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier
            .wrapContentHeight(Alignment.Bottom)
            .background(MixinAppTheme.colors.backgroundWindow),
    ) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            LazyRow {
                items(6) { index ->
                    val hasContent = index < pinCode.length
                    AnimatedContent(
                        targetState = hasContent,
                        transitionSpec = {
                            if (targetState > initialState) {
                                slideInVertically { height -> height } + fadeIn() with
                                    slideOutVertically { height -> -height } + fadeOut()
                            } else {
                                slideInVertically { height -> -height } + fadeIn() with
                                    slideOutVertically { height -> height } + fadeOut()
                            }.using(
                                SizeTransform(clip = false)
                            )
                        }
                    ) { b ->
                        Text(
                            "*",
                            modifier = Modifier.size(20.dp),
                            fontWeight = FontWeight.Black,
                            color = if (b) MixinAppTheme.colors.textPrimary else MixinAppTheme.colors.textMinor,
                            fontSize = if (b) 18.sp else 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = !loading,
        ) {
            Box(
                modifier = Modifier
                    .background(MixinAppTheme.colors.backgroundGray)
                    .height(36.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    color = MixinAppTheme.colors.textMinor,
                    text = stringResource(id = R.string.Secured_by_TIP),
                    fontSize = 12.sp
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .onSizeChanged {
                        size = it
                    }
            ) {
                LazyVerticalGrid(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                    columns = GridCells.Fixed(3),
                    content = {
                        items(list.size) { index ->
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .height(context.pxToDp(size.toSize().height / 4).dp - 1.dp)
                                    .background(
                                        if (index == 11) MixinAppTheme.colors.backgroundDark else MixinAppTheme.colors.background
                                    )
                                    .run {
                                        if (index != 9) {
                                            clickable {
                                                context.tickVibrate()
                                                if (index == 11) {
                                                    if (pinCode.isNotEmpty()) {
                                                        pinCode =
                                                            pinCode.substring(
                                                                0,
                                                                pinCode.length - 1
                                                            )
                                                    }
                                                } else if (pinCode.length < 6) {
                                                    pinCode += list[index]
                                                    if (pinCode.length == 6) {
                                                        loading = true
                                                        callback(pinCode)
                                                        coroutineScope.launch {
                                                            delay(5000)
                                                            loading = false
                                                            pinCode = ""
                                                        }
                                                    }
                                                } else {
                                                    loading = true
                                                    callback(pinCode)
                                                    coroutineScope.launch {
                                                        delay(5000)
                                                        loading = false
                                                        pinCode = ""
                                                    }
                                                }
                                            }
                                        } else {
                                            this
                                        }
                                    }
                            ) {
                                if (index == 11) {
                                    Image(
                                        painter = painterResource(R.drawable.ic_delete),
                                        contentDescription = null
                                    )
                                } else {
                                    Text(
                                        text = list[index],
                                        fontSize = 25.sp,
                                        color = MixinAppTheme.colors.textPrimary,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                )
            }

        }
        AnimatedVisibility(
            visible = loading,
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(32.dp)
                        .padding(8.dp),
                    color = MixinAppTheme.colors.accent
                )
            }
        }
    }
}

@Preview
@Composable
fun PinKeyBoardPreview() {
    PinKeyBoard {
        println(it)
    }
}
