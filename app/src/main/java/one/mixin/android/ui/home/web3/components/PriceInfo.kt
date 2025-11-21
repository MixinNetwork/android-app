
package one.mixin.android.ui.home.web3.components


import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.numberFormat8
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun PriceInfo(
    fromToken: SwapToken,
    toToken: SwapToken?,
    isLoading: Boolean,
    exchangeRate: BigDecimal,
    onPriceExpired: () -> Unit
) {
    var isPriceReverse by remember {
        mutableStateOf(
            false
        )
    }
    var quoteCountDown by remember { mutableFloatStateOf(0f) }

    LaunchedEffect("${fromToken.assetId}-${toToken?.assetId}") {
        isPriceReverse = fromToken.assetId in Constants.AssetId.usdcAssets || fromToken.assetId in Constants.AssetId.usdtAssets
    }

    LaunchedEffect("${fromToken.assetId}-${toToken?.assetId}-${exchangeRate}") {
        while (isActive) {
            quoteCountDown = 0f
            while (isActive && quoteCountDown < 1f) { // 10s
                delay(100)
                quoteCountDown += 0.01f
            }
            onPriceExpired()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isPriceReverse) {
                    "1 ${toToken?.symbol} ≈ ${BigDecimal.ONE.divide(exchangeRate,  8, RoundingMode.HALF_UP).numberFormat8()} ${fromToken.symbol}"
                } else {
                    "1 ${fromToken.symbol} ≈ ${exchangeRate.numberFormat8()} ${toToken?.symbol}"
                },
                maxLines = 1,
                style =
                    TextStyle(
                        fontWeight = FontWeight.W400,
                        color = MixinAppTheme.colors.textAssist,
                    ),
            )
            Spacer(modifier = Modifier.width(4.dp))
            if (!isLoading)
                CircularProgressIndicator(
                    progress = quoteCountDown,
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 2.dp,
                    color = MixinAppTheme.colors.textPrimary,
                    backgroundColor = MixinAppTheme.colors.textAssist,
                )
        }
        if (isLoading) {
            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .height(24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(12.dp)
                        .height(24.dp),
                    color = MixinAppTheme.colors.accent,
                    strokeWidth = 2.dp,
                )
            }
        } else {
            Icon(
                modifier =
                    Modifier
                        .size(24.dp)
                        .rotate(90f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            isPriceReverse = !isPriceReverse
                        },
                painter = painterResource(id = R.drawable.ic_switch),
                contentDescription = null,
                tint = MixinAppTheme.colors.icon,
            )
        }
    }
}