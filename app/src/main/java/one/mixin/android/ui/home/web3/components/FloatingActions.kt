package one.mixin.android.ui.home.web3.components


import one.mixin.android.Constants
import one.mixin.android.api.response.web3.SwapToken
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
            val priceActions = priceQuickActions(
                fromToken = fromToken,
                toToken = toToken,
                isPriceInverted = isPriceInverted,
                onSetPriceMultiplier = onSetPriceMultiplier,
                onMarketPriceClick = onMarketPriceClick,
                onDone = onDone,
                doneLabel = stringResource(R.string.Done),
            )
            if (priceActions.size == 4) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MixinAppTheme.colors.backgroundWindow)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    priceActions.forEach { action ->
                        InputAction(
                            text = action.label,
                            showBorder = action.showBorder,
                            onAction = action.onClick,
                        )
                    }
                }
            } else {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MixinAppTheme.colors.backgroundWindow)
                ) {
                    val buttonSpacing = 3.dp
                    val minButtonWidth = 72.dp
                    val availableWidth = maxWidth - 16.dp
                    val calculatedButtonWidth = (availableWidth - buttonSpacing * (priceActions.size - 1)) / priceActions.size
                    val buttonWidth = if (calculatedButtonWidth > minButtonWidth) calculatedButtonWidth else minButtonWidth

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Start,
                    ) {
                        priceActions.forEachIndexed { index, action ->
                            InputAction(
                                text = action.label,
                                modifier = Modifier.widthIn(min = buttonWidth),
                                showBorder = action.showBorder,
                                horizontalPadding = 14.dp,
                                verticalPadding = 6.dp,
                                fontSize = 13.sp,
                                onAction = action.onClick,
                            )
                            if (index != priceActions.lastIndex) {
                                Spacer(modifier = Modifier.width(buttonSpacing))
                            }
                        }
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

private data class PriceQuickAction(
    val label: String,
    val showBorder: Boolean = true,
    val onClick: () -> Unit,
)

private fun priceQuickActions(
    fromToken: SwapToken?,
    toToken: SwapToken?,
    isPriceInverted: Boolean,
    onSetPriceMultiplier: (Float?) -> Unit,
    onMarketPriceClick: (() -> Unit)?,
    onDone: () -> Unit,
    doneLabel: String,
): List<PriceQuickAction> {
    val isFromUsd = fromToken.isUsdToken()
    val isToUsd = toToken.isUsdToken()
    val marketAction = PriceQuickAction("market") {
        onSetPriceMultiplier(1.0f)
        onMarketPriceClick?.invoke()
    }
    val decreaseActions = listOf(
        PriceQuickAction("-10%") {
            onSetPriceMultiplier(displayPriceMultiplier(0.9f, isPriceInverted))
        },
        PriceQuickAction("-20%") {
            onSetPriceMultiplier(displayPriceMultiplier(0.8f, isPriceInverted))
        },
    )
    val increaseActions = listOf(
        PriceQuickAction("+10%") {
            onSetPriceMultiplier(displayPriceMultiplier(1.1f, isPriceInverted))
        },
        PriceQuickAction("+20%") {
            onSetPriceMultiplier(displayPriceMultiplier(1.2f, isPriceInverted))
        },
    )

    return if (isFromUsd == isToUsd) {
        listOf(
            decreaseActions[1],
            decreaseActions[0],
            marketAction,
            increaseActions[0],
            increaseActions[1],
        )
    } else if (isToUsd) {
        buildList {
            add(marketAction)
            addAll(increaseActions)
            add(PriceQuickAction(doneLabel, showBorder = false, onClick = onDone))
        }
    } else {
        buildList {
            add(marketAction)
            addAll(decreaseActions)
            add(PriceQuickAction(doneLabel, showBorder = false, onClick = onDone))
        }
    }
}

private fun SwapToken?.isUsdToken(): Boolean {
    val assetId = this?.assetId ?: return false
    return assetId in Constants.AssetId.usdtAssets || assetId in Constants.AssetId.usdcAssets
}
