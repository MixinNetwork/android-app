package one.mixin.android.ui.home.web3.components


import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.home.web3.trade.FocusedField
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun FloatingActions(
    focusedField: FocusedField,
    fromBalance: String?,
    isPriceInverted: Boolean,
    onSetInput: (String) -> Unit,
    onSetPriceMultiplier: (Float?) -> Unit,
    onDone: () -> Unit,
    onMarketPriceClick: (() -> Unit)? = null,
) {
    val effectiveField = if (focusedField == FocusedField.NONE) FocusedField.IN_AMOUNT else focusedField
    when (effectiveField) {
        FocusedField.IN_AMOUNT, FocusedField.OUT_AMOUNT -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MixinAppTheme.colors.backgroundWindow)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val balance = fromBalance?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                InputAction("25%", showBorder = true) {
                    if (balance > BigDecimal.ZERO) {
                        onSetInput((balance * BigDecimal("0.25")).stripTrailingZeros().toPlainString())
                    } else {
                        onSetInput("")
                    }
                }
                InputAction("50%", showBorder = true) {
                    if (balance > BigDecimal.ZERO) {
                        onSetInput((balance * BigDecimal("0.5")).stripTrailingZeros().toPlainString())
                    } else {
                        onSetInput("")
                    }
                }
                InputAction(stringResource(R.string.Max), showBorder = true) {
                    if (balance > BigDecimal.ZERO) {
                        onSetInput(balance.stripTrailingZeros().toPlainString())
                    } else {
                        onSetInput("")
                    }
                }
                InputAction(stringResource(R.string.Done), showBorder = false) { onDone() }
            }
        }
        FocusedField.PRICE -> {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MixinAppTheme.colors.backgroundWindow)
            ) {
                val buttonSpacing = 3.dp
                val minButtonWidth = 72.dp
                val availableWidth = maxWidth - 16.dp
                val calculatedButtonWidth = (availableWidth - buttonSpacing * 4) / 5
                val buttonWidth = if (calculatedButtonWidth > minButtonWidth) calculatedButtonWidth else minButtonWidth

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Start,
                ) {
                    InputAction(
                        text = "-20%",
                        modifier = Modifier.widthIn(min = buttonWidth),
                        showBorder = true,
                        horizontalPadding = 14.dp,
                        verticalPadding = 6.dp,
                        fontSize = 13.sp,
                    ) {
                        onSetPriceMultiplier(displayPriceMultiplier(0.8f, isPriceInverted))
                    }
                    Spacer(modifier = Modifier.width(buttonSpacing))
                    InputAction(
                        text = "-10%",
                        modifier = Modifier.widthIn(min = buttonWidth),
                        showBorder = true,
                        horizontalPadding = 14.dp,
                        verticalPadding = 6.dp,
                        fontSize = 13.sp,
                    ) {
                        onSetPriceMultiplier(displayPriceMultiplier(0.9f, isPriceInverted))
                    }
                    Spacer(modifier = Modifier.width(buttonSpacing))
                    InputAction(
                        text = "market",
                        modifier = Modifier.widthIn(min = buttonWidth),
                        showBorder = true,
                        horizontalPadding = 14.dp,
                        verticalPadding = 6.dp,
                        fontSize = 13.sp,
                    ) {
                        onSetPriceMultiplier(1.0f)
                        onMarketPriceClick?.invoke()
                    }
                    Spacer(modifier = Modifier.width(buttonSpacing))
                    InputAction(
                        text = "+10%",
                        modifier = Modifier.widthIn(min = buttonWidth),
                        showBorder = true,
                        horizontalPadding = 14.dp,
                        verticalPadding = 6.dp,
                        fontSize = 13.sp,
                    ) {
                        onSetPriceMultiplier(displayPriceMultiplier(1.1f, isPriceInverted))
                    }
                    Spacer(modifier = Modifier.width(buttonSpacing))
                    InputAction(
                        text = "+20%",
                        modifier = Modifier.widthIn(min = buttonWidth),
                        showBorder = true,
                        horizontalPadding = 14.dp,
                        verticalPadding = 6.dp,
                        fontSize = 13.sp,
                    ) {
                        onSetPriceMultiplier(displayPriceMultiplier(1.2f, isPriceInverted))
                    }
                }
            }
        }
        else -> {}
    }
}

private fun displayPriceMultiplier(displayMultiplier: Float, isPriceInverted: Boolean): Float {
    if (!isPriceInverted) return displayMultiplier

    return BigDecimal.ONE
        .divide(BigDecimal(displayMultiplier.toString()), 8, RoundingMode.HALF_UP)
        .toFloat()
}
