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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.web3.QuoteResult
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.home.web3.trade.SwapViewModel
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun PriceInputArea(
    modifier: Modifier = Modifier,
    fromToken: SwapToken?,
    toToken: SwapToken?,
    lastOrderTime: Long?,
    priceMultiplier: Float?,

    onStandardPriceChanged: (String) -> Unit,
) {
    val viewModel = hiltViewModel<SwapViewModel>()

    val context = LocalContext.current
    var isPriceInverted by remember { mutableStateOf(false) }
    var standardPrice by remember { mutableStateOf("") }
    var marketPrice by remember { mutableStateOf<BigDecimal?>(null) }
    var isPriceLoading by remember { mutableStateOf(false) }
    
    LaunchedEffect(fromToken, toToken) {
        val isFromUsd = fromToken?.assetId?.let { id ->
            Constants.AssetId.usdtAssets.containsKey(id) || Constants.AssetId.usdcAssets.containsKey(id)
        } == true
        val isToUsd = toToken?.assetId?.let { id ->
            Constants.AssetId.usdtAssets.containsKey(id) || Constants.AssetId.usdcAssets.containsKey(id)
        } == true
        isPriceInverted = isFromUsd && !isToUsd
    }
    
    LaunchedEffect(priceMultiplier) {
        if (priceMultiplier != null && marketPrice != null && marketPrice!! > BigDecimal.ZERO) {
            val newPrice = marketPrice!!.multiply(BigDecimal(priceMultiplier.toString()))
                .setScale(8, RoundingMode.HALF_UP)
            val priceString = newPrice.stripTrailingZeros().toPlainString()
            standardPrice = priceString
            onStandardPriceChanged(priceString)
        }
    }
    
    LaunchedEffect(fromToken, toToken, lastOrderTime) {
        marketPrice = null
        standardPrice = ""
        onStandardPriceChanged("")

        val fromT = fromToken
        val toT = toToken
        if (fromT != null && toT != null) {
            // Prefer MarketDao prices
            var fromMarket = viewModel.checkMarketById(fromT.assetId, false)
            var toMarket = viewModel.checkMarketById(toT.assetId, false)
            var fromP = fromMarket?.currentPrice?.toBigDecimalOrNull()
            var toP = toMarket?.currentPrice?.toBigDecimalOrNull()
            if (fromP != null && toP != null && toP > BigDecimal.ZERO) {
                val price = fromP.divide(toP, 8, RoundingMode.HALF_UP)
                marketPrice = price
                val priceString = price.stripTrailingZeros().toPlainString()
                standardPrice = priceString
                onStandardPriceChanged(priceString)
                
                fromMarket = viewModel.checkMarketById(fromT.assetId, true)
                toMarket = viewModel.checkMarketById(toT.assetId, true)
                fromP = fromMarket?.currentPrice?.toBigDecimalOrNull()
                toP = toMarket?.currentPrice?.toBigDecimalOrNull()
                if (fromP != null && toP != null && toP > BigDecimal.ZERO) {
                    val updatedPrice = fromP.divide(toP, 8, RoundingMode.HALF_UP)
                    if (price != updatedPrice) {
                        marketPrice = updatedPrice
                        val updatedPriceString = updatedPrice.stripTrailingZeros().toPlainString()
                        standardPrice = updatedPriceString
                        onStandardPriceChanged(updatedPriceString)
                    }
                }
            } else {
                isPriceLoading = true
                val amount = runCatching { fromT.toLongAmount("1").toString() }.getOrElse { "1" }
                viewModel.quote(context, fromT.symbol, fromT.assetId, toT.assetId, amount, "")
                    .onSuccess { q ->
                        val rate = runCatching { parseRateFromQuote(q) }.getOrNull() ?: BigDecimal.ZERO
                        if (rate > BigDecimal.ZERO) {
                            val price = rate.setScale(8, RoundingMode.HALF_UP)
                            marketPrice = price
                            val priceString = price.stripTrailingZeros().toPlainString()
                            standardPrice = priceString
                            onStandardPriceChanged(priceString)
                        } else {
                            standardPrice = ""
                            onStandardPriceChanged("")
                        }
                        isPriceLoading = false
                    }
                    .onFailure {
                        standardPrice = ""
                        onStandardPriceChanged("")
                        isPriceLoading = false
                    }
            }
        } else {
            isPriceLoading = false
        }
    }
    
    val displayPrice = remember(standardPrice, isPriceInverted) {
        val price = standardPrice.toBigDecimalOrNull()
        if (isPriceInverted && price != null && price > BigDecimal.ZERO) {
            BigDecimal.ONE.divide(price, 8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
        } else {
            standardPrice
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
        onInputChanged = { userInput ->
            val inputPrice = userInput.toBigDecimalOrNull()
            val standardPriceValue = if (isPriceInverted && inputPrice != null && inputPrice > BigDecimal.ZERO) {
                BigDecimal.ONE.divide(inputPrice, 8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
            } else {
                userInput
            }
            standardPrice = standardPriceValue
            onStandardPriceChanged(standardPriceValue)
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
                                .clickable { isPriceInverted = !isPriceInverted }
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
