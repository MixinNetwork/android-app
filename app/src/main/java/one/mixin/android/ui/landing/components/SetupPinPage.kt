package one.mixin.android.ui.landing.components

import PageScaffold
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.pxToDp
import one.mixin.android.extension.tickVibrate

@Composable
fun SetupPinPage(pop: () -> Unit, next: () -> Unit) {
    val context = LocalContext.current
    var size by remember { mutableStateOf(IntSize.Zero) }
    var pinCode by remember { mutableStateOf("") }
    var pinCodeAttempts by remember { mutableIntStateOf(0) }
    var firstPinCode by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val list = listOf(
        "1",
        "2",
        "3",
        "4",
        "5",
        "6",
        "7",
        "8",
        "9",
        "",
        "0",
        "<<",
    )

    PageScaffold(
        title = "",
        verticalScrollable = false,
        actions = {
            IconButton(onClick = {}) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_support),
                    contentDescription = null,
                    tint = MixinAppTheme.colors.icon,
                )
            }
        },
        pop = pop,
    ) {
        Spacer(modifier = Modifier.height(70.dp))

        AnimatedContent(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            targetState = pinCodeAttempts,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally { w -> w } + fadeIn() togetherWith
                        slideOutHorizontally { w -> -w } + fadeOut()
                } else {
                    slideInHorizontally { w -> -w } + fadeIn() togetherWith
                        slideOutHorizontally { w -> w } + fadeOut()
                }.using(
                    SizeTransform(clip = false)
                )
            }, label = "title"
        ) { count ->
            Text(
                stringResource(
                    if (count <= 0) {
                        R.string.Set_up_pin_desc_1
                    } else {
                        R.string.Set_up_pin_desc_2
                    }
                ),
                color = MixinAppTheme.colors.textPrimary,
                minLines = 2,
                fontWeight = FontWeight.W600,
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
                lineHeight = 27.sp
            )
        }

        Spacer(modifier = Modifier.height(65.dp))
        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(204.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(6) { index ->
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(
                            if (index < pinCode.length) MixinAppTheme.colors.textPrimary
                            else Color.Transparent
                        )
                        .border(1.dp, MixinAppTheme.colors.textPrimary, CircleShape)
                )
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
        AnimatedContent(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            targetState = pinCodeAttempts,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally { w -> w } + fadeIn() togetherWith
                        slideOutHorizontally { w -> -w } + fadeOut()
                } else {
                    slideInHorizontally { w -> -w } + fadeIn() togetherWith
                        slideOutHorizontally { w -> w } + fadeOut()
                }.using(
                    SizeTransform(clip = false)
                )
            }, label = "title"
        ) { count ->
            Text(
                when (count) {
                    0 -> ""
                    1 -> stringResource(R.string.Set_up_pin_tip_1)
                    else -> stringResource(R.string.Set_up_pin_tip_2)
                },
                color = MixinAppTheme.colors.red,
                minLines = 2,
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .alpha(if (pinCodeAttempts > 0) 1f else 0f)
                .padding(horizontal = 36.dp)
        ) {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                onClick = {
                    next()
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    backgroundColor = if (pinCodeAttempts == 2 && pinCode == firstPinCode) MixinAppTheme.colors.accent else MixinAppTheme.colors.backgroundGray
                ),
                shape = RoundedCornerShape(32.dp),
                elevation = ButtonDefaults.elevation(
                    pressedElevation = 0.dp,
                    defaultElevation = 0.dp,
                    hoveredElevation = 0.dp,
                    focusedElevation = 0.dp,
                ),
                enabled = pinCodeAttempts == 2 && pinCode == firstPinCode
            ) {
                Text(
                    text = stringResource(R.string.Next),
                    color = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier =
            Modifier
                .wrapContentHeight()
                .heightIn(120.dp, 240.dp)
                .onSizeChanged {
                    size = it
                },
        ) {
            LazyVerticalGrid(
                modifier =
                Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                columns = GridCells.Fixed(3),
                content = {
                    items(list.size) { index ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier =
                            Modifier
                                .height(
                                    context.pxToDp(
                                        (
                                            size.toSize().height -
                                                context.dpToPx(
                                                    40f,
                                                )
                                            ) / 4,
                                    ).dp,
                                )
                                .clip(shape = RoundedCornerShape(8.dp))
                                .background(
                                    when (index) {
                                        11 -> MixinAppTheme.colors.backgroundDark
                                        9 -> Color.Transparent
                                        else -> MixinAppTheme.colors.background
                                    },
                                )
                                .run {
                                    clickable {
                                        context.tickVibrate()
                                        if (index == 11) {
                                            if (pinCode.isNotEmpty()) {
                                                pinCode =
                                                    pinCode.substring(
                                                        0,
                                                        pinCode.length - 1,
                                                    )
                                            }
                                        } else if (pinCode.length < 6) {
                                            pinCode += list[index]
                                            if (pinCode.length == 6) {
                                                if (pinCodeAttempts < 2) {
                                                    pinCodeAttempts++
                                                    if (pinCodeAttempts == 1) {
                                                        firstPinCode = pinCode
                                                    } else if (pinCode != firstPinCode) {
                                                        pinCodeAttempts = 0
                                                    }
                                                    coroutineScope.launch {
                                                        delay(100)
                                                        pinCode = ""
                                                    }
                                                } else {
                                                    if (pinCode != firstPinCode) {
                                                        pinCodeAttempts = 0
                                                        coroutineScope.launch {
                                                            delay(100)
                                                            pinCode = ""
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                        ) {
                            if (index == 11) {
                                Image(
                                    painter = painterResource(R.drawable.ic_delete),
                                    contentDescription = null,
                                )
                            } else if (index != 9) {
                                Text(
                                    text = list[index],
                                    fontSize = 24.sp,
                                    color = MixinAppTheme.colors.textPrimary,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                },
            )
        }
    }
}
