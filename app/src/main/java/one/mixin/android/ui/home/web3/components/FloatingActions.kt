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
    onSetLimitPrice: (String) -> Unit,
    onDone: () -> Unit,
) {
    when (focusedField) {
        FocusedField.AMOUNT -> {
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
                val marketPrice = remember(fromToken, toToken) {
                    val fromPrice = fromToken?.price?.toBigDecimalOrNull()
                    val toPrice = toToken?.price?.toBigDecimalOrNull()
                    if (fromPrice != null && toPrice != null && toPrice > BigDecimal.ZERO) {
                        fromPrice.divide(toPrice, 8, RoundingMode.HALF_UP)
                    } else {
                        null
                    }
                }
                InputAction(stringResource(R.string.market_price), showBorder = true) {
                    marketPrice?.let { onSetLimitPrice(it.stripTrailingZeros().toPlainString()) }
                }
                InputAction("+10%", showBorder = true) {
                    marketPrice?.let {
                        val newPrice = it.multiply(BigDecimal("1.1"))
                        onSetLimitPrice(newPrice.stripTrailingZeros().toPlainString())
                    }
                }
                InputAction("+20%", showBorder = true) {
                    marketPrice?.let {
                        val newPrice = it.multiply(BigDecimal("1.2"))
                        onSetLimitPrice(newPrice.stripTrailingZeros().toPlainString())
                    }
                }
                InputAction(stringResource(R.string.Done), showBorder = false) { onDone() }
            }
        }
        else -> {}
    }
}

