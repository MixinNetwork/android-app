package one.mixin.android.ui.home.web3.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.web3.QuoteResult
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.home.web3.trade.SwapViewModel
import one.mixin.android.ui.home.web3.trade.limitTradeInputDecimalPlaces
import one.mixin.android.ui.home.web3.trade.tradeInputMaxDecimalPlaces
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun PriceInputArea(
    modifier: Modifier = Modifier,
    fromToken: SwapToken?,
    toToken: SwapToken?,
    lastOrderTime: Long?,
    priceMultiplier: Float?,
    isPriceInverted: Boolean,
    onPriceInvertedChange: (Boolean) -> Unit,
    onStandardPriceChanged: (String) -> Unit,
) {
    val viewModel = hiltViewModel<SwapViewModel>()

    val context = LocalContext.current

    // Display price shown in the input field, initialized from market price
    var displayPrice by remember { mutableStateOf("") }
    
    var marketPrice by remember { mutableStateOf<BigDecimal?>(null) }
    
    // Loading state when fetching price from API
    var isPriceLoading by remember { mutableStateOf(false) }

    val displayToken = if (isPriceInverted) fromToken else toToken
    val displayMaxDecimalPlaces = displayToken.tradeInputMaxDecimalPlaces()
    val standardMaxDecimalPlaces = toToken.tradeInputMaxDecimalPlaces()

    fun formatLimitedPrice(value: BigDecimal, maxDecimalPlaces: Int): String {
        return value
            .setScale(maxDecimalPlaces.coerceAtLeast(0), RoundingMode.DOWN)
            .stripTrailingZeros()
            .toPlainString()
    }

    fun formatStandardPrice(value: BigDecimal): String =
        formatLimitedPrice(value, standardMaxDecimalPlaces)

    fun formatDisplayPrice(standardPrice: BigDecimal): String {
        return if (isPriceInverted && standardPrice > BigDecimal.ZERO) {
            formatLimitedPrice(
                BigDecimal.ONE.divide(standardPrice, displayMaxDecimalPlaces, RoundingMode.DOWN),
                displayMaxDecimalPlaces,
            )
        } else {
            formatLimitedPrice(standardPrice, displayMaxDecimalPlaces)
        }
    }

    fun limitedStandardPriceFromDisplay(displayValue: String): String {
        val inputPrice = displayValue.toBigDecimalOrNull()
        return if (isPriceInverted && inputPrice != null && inputPrice > BigDecimal.ZERO) {
            formatLimitedPrice(
                BigDecimal.ONE.divide(inputPrice, standardMaxDecimalPlaces, RoundingMode.DOWN),
                standardMaxDecimalPlaces,
            )
        } else {
            limitTradeInputDecimalPlaces(displayValue, standardMaxDecimalPlaces)
        }
    }

    LaunchedEffect(fromToken, toToken) {
        val isFromUsd = fromToken?.assetId?.let { id ->
            Constants.AssetId.usdtAssets.containsKey(id) || Constants.AssetId.usdcAssets.containsKey(id)
        } == true
        val isToUsd = toToken?.assetId?.let { id ->
            Constants.AssetId.usdtAssets.containsKey(id) || Constants.AssetId.usdcAssets.containsKey(id)
        } == true
        onPriceInvertedChange(isFromUsd && !isToUsd)
    }
    
    LaunchedEffect(priceMultiplier) {
        if (priceMultiplier != null && marketPrice != null && marketPrice!! > BigDecimal.ZERO) {
            val newPrice = marketPrice!!.multiply(BigDecimal(priceMultiplier.toString()))
            val priceString = formatStandardPrice(newPrice)
            onStandardPriceChanged(priceString)
            displayPrice = formatDisplayPrice(newPrice)
        }
    }
    
    LaunchedEffect(fromToken, toToken, lastOrderTime, isPriceInverted) {
        marketPrice = null
        displayPrice = ""
        onStandardPriceChanged("")

        val fromT = fromToken
        val toT = toToken
        if (fromT != null && toT != null) {
            var fromMarket = viewModel.checkMarketById(fromT.assetId, false)
            var toMarket = viewModel.checkMarketById(toT.assetId, false)
            var fromP = fromMarket?.currentPrice?.toBigDecimalOrNull()
            var toP = toMarket?.currentPrice?.toBigDecimalOrNull()
            if (fromP != null && toP != null && toP > BigDecimal.ZERO) {
                val price = fromP.multiply(BigDecimal(0.99)).divide(toP, 8, RoundingMode.HALF_UP)
                marketPrice = price
                val priceString = formatStandardPrice(price)
                onStandardPriceChanged(priceString)
                displayPrice = formatDisplayPrice(price)
                
                fromMarket = viewModel.checkMarketById(fromT.assetId, true)
                toMarket = viewModel.checkMarketById(toT.assetId, true)
                fromP = fromMarket?.currentPrice?.toBigDecimalOrNull()
                toP = toMarket?.currentPrice?.toBigDecimalOrNull()
                if (fromP != null && toP != null && toP > BigDecimal.ZERO) {
                    val updatedPrice = fromP.multiply(BigDecimal(0.99)).divide(toP, 8, RoundingMode.HALF_UP)
                    if (price != updatedPrice) {
                        marketPrice = updatedPrice
                        val updatedPriceString = formatStandardPrice(updatedPrice)
                        onStandardPriceChanged(updatedPriceString)
                        displayPrice = formatDisplayPrice(updatedPrice)
                    }
                }
            } else {
                isPriceLoading = true
                val amount = runCatching { fromT.toLongAmount("1").toString() }.getOrElse { "1" }
                viewModel.quote(context, fromT.symbol, fromT.assetId, toT.assetId, amount, "")
                    .onSuccess { q ->
                        val rate = runCatching { parseRateFromQuote(q) }.getOrNull() ?: BigDecimal.ZERO
                        if (rate > BigDecimal.ZERO) {
                            val price = rate
                            marketPrice = price
                            val priceString = formatStandardPrice(price)
                            onStandardPriceChanged(priceString)
                            displayPrice = formatDisplayPrice(price)
                        } else {
                            displayPrice = ""
                            onStandardPriceChanged("")
                        }
                        isPriceLoading = false
                    }
                    .onFailure {
                        displayPrice = ""
                        onStandardPriceChanged("")
                        isPriceLoading = false
                    }
            }
        } else {
            isPriceLoading = false
        }
    }

    val priceDisplayState = remember(fromToken, toToken, isPriceInverted, isPriceLoading) {
        PriceDisplayState(
            fromToken = fromToken,
            toToken = toToken,
            isPriceInverted = isPriceInverted,
            isPriceLoading = isPriceLoading,
        )
    }
    InputArea(
        modifier = modifier,
        token = priceDisplayState.displayToken,
        text = displayPrice,
        title = stringResource(id = R.string.limit_price, priceDisplayState.displayChainName),
        readOnly = false,
        selectClick = null,
        maxDecimalPlaces = displayMaxDecimalPlaces,
        onInputChanged = { userInput ->
            val limitedDisplayPrice = limitTradeInputDecimalPlaces(userInput, displayMaxDecimalPlaces)
            displayPrice = limitedDisplayPrice
            onStandardPriceChanged(limitedStandardPriceFromDisplay(limitedDisplayPrice))
        },
        inlineEndCompose = if (priceDisplayState.isPriceLoading) {
            {
                CircularProgressIndicator(
                    modifier = Modifier.size(26.dp),
                    color = MixinAppTheme.colors.textPrimary,
                    strokeWidth = 4.dp,
                )
            }
        } else null,
        bottomCompose = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                val priceRatioText = priceDisplayState.formatPriceRatio(displayPrice)
                if (priceRatioText != null) {
                    Text(
                        text = priceRatioText,
                        color = MixinAppTheme.colors.textAssist,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    if (priceRatioText != "-") {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.ic_price_switch),
                            contentDescription = null,
                            tint = MixinAppTheme.colors.textAssist,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onPriceInvertedChange(!isPriceInverted) }
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = priceDisplayState.displayTokenName,
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis,
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = MixinAppTheme.colors.textAssist,
                        textAlign = TextAlign.Start,
                    ),
                )
            }
        },
    )
}

private fun parseRateFromQuote(q: QuoteResult?): BigDecimal? {
    q ?: return null
    val outputAmount = q.outAmount.toBigDecimalOrNull()
    val inputAmount = q.inAmount.toBigDecimalOrNull()

    if (outputAmount == null || inputAmount == null || inputAmount == BigDecimal.ZERO || outputAmount == BigDecimal.ZERO) {
        return null
    } else {
        return outputAmount.divide(inputAmount)
    }
}
