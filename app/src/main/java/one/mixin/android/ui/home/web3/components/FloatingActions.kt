package one.mixin.android.ui.home.web3.components


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.home.web3.trade.FocusedField
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun FloatingActions(
    focusedField: FocusedField,
    fromBalance: String?,
    fromToken: SwapToken?,
    toToken: SwapToken?,
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MixinAppTheme.colors.backgroundWindow)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                InputAction(stringResource(R.string.market_price), showBorder = true) {
                    onSetPriceMultiplier(1.0f)
                    onMarketPriceClick?.invoke()
                }

                val isFromUsd = fromToken?.assetId?.let { id ->
                    Constants.AssetId.usdtAssets.containsKey(id) || Constants.AssetId.usdcAssets.containsKey(id)
                } == true
                val isToUsd = toToken?.assetId?.let { id ->
                    Constants.AssetId.usdtAssets.containsKey(id) || Constants.AssetId.usdcAssets.containsKey(id)
                } == true

                if (isToUsd && !isFromUsd) {
                    InputAction("+10%", showBorder = true) {
                        onSetPriceMultiplier(displayPriceMultiplier(1.1f, isPriceInverted))
                    }
                    InputAction("+20%", showBorder = true) {
                        onSetPriceMultiplier(displayPriceMultiplier(1.2f, isPriceInverted))
                    }
                } else {
                    InputAction("-10%", showBorder = true) {
                        onSetPriceMultiplier(displayPriceMultiplier(0.9f, isPriceInverted))
                    }
                    InputAction("-20%", showBorder = true) {
                        onSetPriceMultiplier(displayPriceMultiplier(0.8f, isPriceInverted))
                    }
                }
                InputAction(stringResource(R.string.Done), showBorder = false) { onDone() }
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
