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
import androidx.compose.ui.unit.sp
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.compose.theme.MixinAppTheme
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun PriceInputArea(
    modifier: Modifier = Modifier,
    fromToken: SwapToken?,
    toToken: SwapToken?,
    standardPrice: String,
    isPriceLoading: Boolean,
    onStandardPriceChanged: (String) -> Unit,
) {
    var isPriceInverted by remember { mutableStateOf(false) }
    
    LaunchedEffect(fromToken, toToken) {
        val isFromUsd = fromToken?.assetId?.let { id ->
            Constants.AssetId.usdtAssets.containsKey(id) || Constants.AssetId.usdcAssets.containsKey(id)
        } == true
        val isToUsd = toToken?.assetId?.let { id ->
            Constants.AssetId.usdtAssets.containsKey(id) || Constants.AssetId.usdcAssets.containsKey(id)
        } == true
        isPriceInverted = isFromUsd && !isToUsd
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
