@file:OptIn(ExperimentalAnimationApi::class)

package one.mixin.android.ui.tip.wc.sessionproposal

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.pxToDp
import one.mixin.android.extension.tickVibrate
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.ui.setting.ui.compose.booleanValueAsState
import one.mixin.android.ui.setting.ui.theme.MixinAppTheme
import one.mixin.android.ui.tip.wc.WalletConnectBottomSheetDialogFragment.Step
import one.mixin.android.util.BiometricUtil

@Composable
fun WCPinBoard(
    step: Step,
    errorInfo: String?,
    allowBiometric: Boolean,
    signUnavailable: Boolean,
    onNegativeClick: () -> Unit,
    onPositiveClick: () -> Unit,
    onDoneClick: () -> Unit,
    onBiometricClick: (() -> Unit)?,
    onPinComplete: ((String) -> Unit)?,
) {
    val context = LocalContext.current
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val showBiometric = allowBiometric && BiometricUtil.shouldShowBiometric(context)
    val randomKeyboardEnabled by LocalContext.current.defaultSharedPreferences
        .booleanValueAsState(
            key = Constants.Account.PREF_RANDOM,
            defaultValue = false,
        )
    val list =
        if (randomKeyboardEnabled) {
            mutableListOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").apply {
                shuffle()
                add(9, "")
                add("<<")
            }
        } else {
            listOf(
                "1", "2", "3",
                "4", "5", "6",
                "7", "8", "9",
                "", "0", "<<",
            )
        }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var pinCode by remember { mutableStateOf("") }

    AnimatedContent(targetState = step, transitionSpec = {
        if (targetState == Step.Input) {
            (slideInVertically(initialOffsetY = { it }) togetherWith scaleOut() + fadeOut())
        } else if (initialState == Step.Input) {
            if (targetState == Step.Loading) {
                (EnterTransition.None togetherWith ExitTransition.None)
            } else {
                (scaleIn() + fadeIn() togetherWith fadeOut())
            }
        } else {
            (scaleIn() + fadeIn() togetherWith scaleOut() + fadeOut())
        }
    }, label = "") { s ->
        when (s) {
            Step.Error ->
                Column(
                    modifier =
                        Modifier
                            .clickable(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(errorInfo ?: ""))
                                    toast(R.string.copied_to_clipboard)
                                },
                            )
                            .height(200.dp)
                            .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Error",
                        color = MixinAppTheme.colors.red,
                        fontSize = 18.sp,
                    )
                    Text(
                        modifier = Modifier.padding(32.dp, 12.dp, 32.dp, 32.dp),
                        text = errorInfo ?: "",
                        textAlign = TextAlign.Center,
                        color = MixinAppTheme.colors.textPrimary,
                        fontSize = 14.sp,
                    )
                }
            Step.Done ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(modifier = Modifier.height(20.dp))
                    Image(
                        painter = painterResource(id = R.drawable.ic_transfer_done),
                        contentDescription = null,
                    )
                    Box(modifier = Modifier.height(12.dp))
                    Text(text = stringResource(R.string.Success), color = MixinAppTheme.colors.textMinor)
                    Box(modifier = Modifier.height(40.dp))
                    Button(
                        onClick = { onDoneClick.invoke() },
                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                backgroundColor = MixinAppTheme.colors.accent,
                            ),
                        contentPadding = PaddingValues(horizontal = 28.dp),
                        shape = RoundedCornerShape(40.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.Done),
                            color = Color.White,
                        )
                    }
                    Box(modifier = Modifier.height(32.dp))
                }
            Step.Sign ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Box(modifier = Modifier.height(20.dp))
                    Button(
                        modifier = Modifier.widthIn(min = 100.dp),
                        onClick = {
                            if (!signUnavailable) {
                                onPositiveClick.invoke()
                            }
                        },
                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                backgroundColor = MixinAppTheme.colors.accent,
                            ),
                        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 11.dp),
                        shape = RoundedCornerShape(40.dp),
                    ) {
                        if (signUnavailable) {
                            CircularProgressIndicator(
                                modifier =
                                    Modifier
                                        .size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                text = stringResource(id = R.string.sign_by_pin),
                                color = Color.White,
                                fontSize = 16.sp,
                            )
                        }
                    }
                    Box(modifier = Modifier.height(24.dp))
                    TextButton(
                        onClick = { onNegativeClick() },
                        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 11.dp),
                        shape = RoundedCornerShape(40.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.Cancel),
                            color = Color(0xFF4B7CDD),
                            fontSize = 16.sp,
                        )
                    }
                    Box(modifier = Modifier.height(32.dp))
                }
            else ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedContent(targetState = step, transitionSpec = {
                        (fadeIn() togetherWith fadeOut())
                    }, label = "") {
                        if (step == Step.Input) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight(),
                            ) {
                                Box(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier =
                                        Modifier
                                            .padding(8.dp)
                                            .fillMaxWidth(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    LazyRow(
                                        modifier = Modifier.height(20.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        items(6) { index ->
                                            val hasContent = index < pinCode.length
                                            AnimatedContent(
                                                targetState = hasContent,
                                                transitionSpec = {
                                                    if (targetState > initialState) {
                                                        scaleIn() + fadeIn() togetherWith scaleOut() + fadeOut()
                                                    } else {
                                                        scaleIn() + fadeIn() togetherWith scaleOut() + fadeOut()
                                                    }.using(
                                                        SizeTransform(clip = false),
                                                    )
                                                },
                                                label = "",
                                            ) { b ->
                                                Text(
                                                    "*",
                                                    modifier =
                                                        Modifier
                                                            .width(24.dp),
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (b) MixinAppTheme.colors.textPrimary else MixinAppTheme.colors.textMinor,
                                                    fontSize = if (b) 18.sp else 12.sp,
                                                    textAlign = TextAlign.Center,
                                                )
                                            }
                                        }
                                    }
                                }
                                if (showBiometric) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier =
                                            Modifier
                                                .padding(horizontal = 12.dp, vertical = 3.dp)
                                                .clip(
                                                    shape = RoundedCornerShape(4.dp),
                                                )
                                                .clickable { onBiometricClick?.invoke() },
                                    ) {
                                        Image(
                                            painter = painterResource(R.drawable.ic_biometric),
                                            contentDescription = null,
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = stringResource(R.string.Verify_by_Biometric),
                                            color = MixinAppTheme.colors.textBlue,
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        } else {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(94.dp),
                            ) {
                                CircularProgressIndicator(
                                    modifier =
                                        Modifier
                                            .size(32.dp),
                                    color = MixinAppTheme.colors.accent,
                                )
                            }
                        }
                    }
                    AnimatedVisibility(
                        visible = step == Step.Input || step == Step.Loading,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it }),
                    ) {
                        Column(modifier = Modifier.background(MixinAppTheme.colors.backgroundWindow)) {
                            if (Session.getTipPub() != null) {
                                Row(
                                    modifier =
                                        Modifier
                                            .background(MixinAppTheme.colors.backgroundWindow)
                                            .height(36.dp)
                                            .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_secret_tip),
                                        contentDescription = null,
                                        tint = MixinAppTheme.colors.textMinor,
                                    )
                                    Text(
                                        color = MixinAppTheme.colors.textMinor,
                                        text = stringResource(id = R.string.Secured_by_TIP),
                                        fontSize = 12.sp,
                                    )
                                }
                            }
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
                                                            if (step == Step.Input && index != 9) {
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
                                                                            onPinComplete?.invoke(pinCode)
                                                                            pinCode = ""
                                                                        }
                                                                    }
                                                                }
                                                            } else {
                                                                this
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
                }
        }
    }
}

@Preview
@Composable
fun WCPinBoardPreview() {
    WCPinBoard(Step.Input, null, allowBiometric = false, true, {}, {}, {}, null, null)
}
