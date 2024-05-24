package one.mixin.android.ui.home.web3.swap

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun SwapSlippagePage(
    autoSlippage: Boolean,
    slippageBps: Int,
    onCancel: () -> Unit,
    onConfirm: (Boolean, Int) -> Unit,
) {
    val customText = rememberSaveable {
        mutableStateOf(slippageBps.slippageBpsDisplay())
    }
    val auto = remember {
        mutableStateOf(autoSlippage)
    }
    MixinAppTheme {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp, 10.dp, 10.dp, 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(id = R.string.Slippage),
                    style = TextStyle(
                        fontSize = 18.sp,
                        color = MixinAppTheme.colors.textPrimary
                    )
                )
                IconButton(onClick = { onCancel.invoke() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = null,
                        tint = MixinAppTheme.colors.icon,
                    )
                }
            }
            Auto(auto, autoSlippage, slippageBps)
            Custom(auto, customText)
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp, 10.dp),
                onClick = {
                    onConfirm.invoke(auto.value, customText.value.toIntSlippage())
                },
                colors =
                ButtonDefaults.outlinedButtonColors(
                    backgroundColor = MixinAppTheme.colors.accent,
                ),
                shape = RoundedCornerShape(32.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                elevation =
                ButtonDefaults.elevation(
                    pressedElevation = 0.dp,
                    defaultElevation = 0.dp,
                    hoveredElevation = 0.dp,
                    focusedElevation = 0.dp,
                ),
            ) {
                Text(text = stringResource(id = R.string.Confirm), color = Color.White)
            }
        }
    }
}

@Composable
private fun Auto(
    auto: MutableState<Boolean>,
    originAuto: Boolean,
    originBps: Int,
) {
    val context = LocalContext.current
    val text = context.getString(R.string.slippage_auto) + if (originAuto) " (${originBps.slippageBpsDisplay()}%)" else ""
    Column(modifier =
    Modifier
        .fillMaxWidth()
        .wrapContentHeight()
        .padding(20.dp, 10.dp)
        .border(1.dp, color = if (auto.value) MixinAppTheme.colors.textPrimary else MixinAppTheme.colors.textMinor, shape = RoundedCornerShape(12.dp))
        .clickable {
            auto.value = true
        }
        .padding(20.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 18.sp,
                color = MixinAppTheme.colors.textPrimary
            )
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = context.getString(R.string.slippage_auto_desc),
            style = TextStyle(
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textMinor
            )
        )
    }
}

@Composable
private fun Custom(
    auto: MutableState<Boolean>,
    bps: MutableState<String>,
) {
    val context = LocalContext.current
    Column(modifier =
    Modifier
        .fillMaxWidth()
        .wrapContentHeight()
        .padding(20.dp, 10.dp)
        .border(1.dp, color = if (!auto.value) MixinAppTheme.colors.textPrimary else MixinAppTheme.colors.textMinor, shape = RoundedCornerShape(12.dp))
        .clickable {
            auto.value = false
        }
        .padding(20.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = context.getString(R.string.slippage_custom),
            style = TextStyle(
                fontSize = 18.sp,
                color = MixinAppTheme.colors.textPrimary
            )
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = context.getString(R.string.slippage_custom_desc),
            style = TextStyle(
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textMinor
            )
        )
        if (!auto.value) {
            val focusRequester = remember { FocusRequester() }
            val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
            val interactionSource = remember { MutableInteractionSource() }
            val focused = remember { mutableStateOf(false) }
            Spacer(modifier = Modifier.height(10.dp))
            BasicTextField(
                value = bps.value,
                onValueChange = {
                    bps.value = it
                },
                modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        if (it.isFocused) {
                            keyboardController?.show()
                        }
                        focused.value = it.isFocused
                    },
                interactionSource = interactionSource,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                textStyle =
                TextStyle(
                    fontSize = 20.sp,
                    color = MixinAppTheme.colors.textPrimary,
                    textAlign = TextAlign.Start
                ),
                cursorBrush = SolidColor(MixinAppTheme.colors.accent),
            ) { innerTextField ->
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, color = if (focused.value) MixinAppTheme.colors.textPrimary else MixinAppTheme.colors.textMinor, RoundedCornerShape(8.dp))
                    .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Box(contentAlignment = Alignment.CenterStart) {
                        innerTextField()
                        if (bps.value.isEmpty()) {
                            Text(
                                text = "0% - 50%",
                                fontSize = 16.sp,
                                color = MixinAppTheme.colors.textSubtitle,
                            )
                        }
                    }
                    Text(
                        text = "%", style = TextStyle(
                            fontSize = 20.sp,
                            color = MixinAppTheme.colors.textPrimary,
                        )
                    )
                }
            }
        }
    }
}

fun Int.slippageBpsDisplay(): String = BigDecimal(this).divide(BigDecimal(100)).setScale(2, RoundingMode.CEILING).stripTrailingZeros().toPlainString()
private fun String.toIntSlippage(): Int {
    return if (this.isBlank()) {
        0
    } else {
        BigDecimal(this).multiply(BigDecimal(100)).toInt()
    }
}

@Preview
@Composable
fun PreviewAuto() {
    Auto(
        auto = remember {
            mutableStateOf(true)
        },
        originAuto = true,
        originBps = 80,
    )
}

@Preview
@Composable
fun PreviewCustom() {
    Custom(
        auto = remember {
            mutableStateOf(false)
        },
        bps = remember {
            mutableStateOf("50")
        }
    )
}