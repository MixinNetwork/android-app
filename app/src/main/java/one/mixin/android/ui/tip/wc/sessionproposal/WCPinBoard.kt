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
import androidx.compose.ui.platform.LocalInspectionMode
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
import one.mixin.android.compose.booleanValueAsState
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.pxToDp
import one.mixin.android.extension.tickVibrate
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.ui.tip.wc.WalletConnectBottomSheetDialogFragment.Step
import one.mixin.android.util.BiometricUtil
import one.mixin.android.widget.components.MixinButton

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
    val isInPreview = LocalInspectionMode.current
    
    val showBiometric = if (isInPreview) false else allowBiometric && BiometricUtil.shouldShowBiometric(context)
    val randomKeyboardEnabled by if (isInPreview) {
        remember { mutableStateOf(false) }
    } else {
        context.defaultSharedPreferences.booleanValueAsState(
            key = Constants.Account.PREF_RANDOM,
            defaultValue = false,
        )
    }

    WCPinBoardContent(
        step = step,
        errorInfo = errorInfo,
        showBiometric = showBiometric,
        signUnavailable = signUnavailable,
        randomKeyboardEnabled = randomKeyboardEnabled,
        onNegativeClick = onNegativeClick,
        onPositiveClick = onPositiveClick,
        onDoneClick = onDoneClick,
        onBiometricClick = onBiometricClick,
        onPinComplete = onPinComplete
    )
}

@Composable
fun WCPinBoardContent(
    step: Step,
    errorInfo: String?,
    showBiometric: Boolean,
    signUnavailable: Boolean,
    randomKeyboardEnabled: Boolean,
    onNegativeClick: () -> Unit,
    onPositiveClick: () -> Unit,
    onDoneClick: () -> Unit,
    onBiometricClick: (() -> Unit)?,
    onPinComplete: ((String) -> Unit)?,
) {
    val context = LocalContext.current
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    
    val list =
        if (randomKeyboardEnabled) {
            mutableListOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").apply {
                shuffle()
                add(9, "")
                add("<<")
            }
        } else {
            listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "<<")
        }

    var pinValue by remember {
        mutableStateOf("")
    }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
    ) {
        if (step == Step.Input || step == Step.Verifying) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                for (i in 1..6) {
                    Box(
                        modifier =
                            Modifier
                                .size(14.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(
                                    if (pinValue.length >= i) {
                                        MixinAppTheme.colors.accent
                                    } else {
                                        MixinAppTheme.colors.backgroundGrayLight
                                    },
                                ),
                    )
                    if (i != 6) {
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (errorInfo != null) {
                    Text(
                        text = errorInfo,
                        color = MixinAppTheme.colors.red,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            var keyboardHeight by remember {
                mutableStateOf(0.dp)
            }

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = keyboardHeight),
            ) {
                if (step == Step.Verifying) {
                    CircularProgressIndicator(
                        modifier =
                            Modifier
                                .size(32.dp)
                                .align(Alignment.Center),
                        color = MixinAppTheme.colors.accent,
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .onSizeChanged {
                                    keyboardHeight = context.pxToDp(it.height).dp
                                },
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(list.size) { index ->
                            val item = list[index]
                            when (item) {
                                "" -> {
                                    if (showBiometric) {
                                        Box(
                                            modifier =
                                                Modifier
                                                    .size(60.dp)
                                                    .clip(RoundedCornerShape(30.dp))
                                                    .clickable {
                                                        onBiometricClick?.invoke()
                                                    },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_biometric),
                                                contentDescription = null,
                                                tint = MixinAppTheme.colors.icon,
                                            )
                                        }
                                    } else {
                                        Box(modifier = Modifier.size(60.dp))
                                    }
                                }
                                "<<" -> {
                                    Box(
                                        modifier =
                                            Modifier
                                                .size(60.dp)
                                                .clip(RoundedCornerShape(30.dp))
                                                .clickable {
                                                    if (pinValue.isNotEmpty()) {
                                                        pinValue = pinValue.substring(0, pinValue.length - 1)
                                                        context.tickVibrate()
                                                    }
                                                },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_backspace),
                                            contentDescription = null,
                                            tint = MixinAppTheme.colors.icon,
                                        )
                                    }
                                }
                                else -> {
                                    Box(
                                        modifier =
                                            Modifier
                                                .size(60.dp)
                                                .clip(RoundedCornerShape(30.dp))
                                                .background(MixinAppTheme.colors.backgroundGrayLight)
                                                .clickable {
                                                    if (pinValue.length < 6) {
                                                        pinValue += item
                                                        context.tickVibrate()
                                                        if (pinValue.length == 6) {
                                                            onPinComplete?.invoke(pinValue)
                                                            pinValue = ""
                                                        }
                                                    }
                                                },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = item,
                                            fontSize = 24.sp,
                                            color = MixinAppTheme.colors.textPrimary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(id = R.string.Forgot_PIN),
                    modifier =
                        Modifier.clickable {
                            context.toast(R.string.forget_pin_tip)
                        },
                    color = MixinAppTheme.colors.accent,
                    fontSize = 14.sp,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        } else {
            Spacer(modifier = Modifier.height(24.dp))
            MixinButton(
                text = stringResource(id = R.string.Done),
                modifier = Modifier.fillMaxWidth(),
                onClick = onDoneClick,
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WCPinBoardPreview() {
    MixinAppTheme {
        WCPinBoardContent(
            step = Step.Input,
            errorInfo = null,
            showBiometric = true,
            signUnavailable = false,
            randomKeyboardEnabled = false,
            onNegativeClick = {},
            onPositiveClick = {},
            onDoneClick = {},
            onBiometricClick = {},
            onPinComplete = {}
        )
    }
}
