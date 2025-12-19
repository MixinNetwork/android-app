package one.mixin.android.ui.home.web3.components



import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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

                val isToUsd = toToken?.assetId?.let { id ->
                    Constants.AssetId.usdtAssets.containsKey(id) || Constants.AssetId.usdcAssets.containsKey(id)
                } == true

                if (isToUsd) {
                    InputAction("+10%", showBorder = true) { onSetPriceMultiplier(1.1f) }
                    InputAction("+20%", showBorder = true) { onSetPriceMultiplier(1.2f) }
                } else {
                    // from is USD or other cases -> -10% / -20%
                    InputAction("-10%", showBorder = true) { onSetPriceMultiplier(0.9f) }
                    InputAction("-20%", showBorder = true) { onSetPriceMultiplier(0.8f) }
                }
                InputAction(stringResource(R.string.Done), showBorder = false) { onDone() }
            }
        }
        else -> {}
    }
}

