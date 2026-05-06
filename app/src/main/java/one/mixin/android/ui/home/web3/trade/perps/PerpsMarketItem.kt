package one.mixin.android.ui.home.web3.trade.perps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.numberFormatCompact
import one.mixin.android.extension.priceFormat
import one.mixin.android.vo.Fiats
import java.math.BigDecimal

@Composable
fun PerpsMarketItem(
    market: PerpsMarket,
    quoteColorReversed: Boolean = false,
    onClick: () -> Unit = {}
) {
    val changePercent = market.changePercent()
    val isPositive = changePercent >= BigDecimal.ZERO
    val changeColor = if (isPositive) {
        if (quoteColorReversed) {
            MixinAppTheme.colors.walletRed
        } else {
            MixinAppTheme.colors.walletGreen
        }
    } else {
        if (quoteColorReversed) {
            MixinAppTheme.colors.walletGreen
        } else {
            MixinAppTheme.colors.walletRed
        }
    }
    val changeText = formatPerpsSignedPercent(changePercent)
    val fiatRate = BigDecimal(Fiats.getRate())
    val formattedPrice = try {
        BigDecimal(market.last).priceFormat()
    } catch (e: Exception) {
        market.last
    }

    val formattedVolume = try {
        BigDecimal(market.volume).multiply(fiatRate).numberFormatCompact()
    } catch (e: Exception) {
        market.volume
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoilImage(
            model = market.iconUrl,
            placeholder = R.drawable.ic_avatar_place_holder,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = market.tokenSymbol,
                        fontSize = 16.sp,
                        color = MixinAppTheme.colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${market.leverage}x",
                        fontSize = 12.sp,
                        color = MixinAppTheme.colors.textAssist,
                        lineHeight = 14.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MixinAppTheme.colors.backgroundGrayLight)
                            .padding(horizontal = 3.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = "$PERPS_USD_SYMBOL$formattedPrice",
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textPrimary,
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.Vol, "${Fiats.getSymbol()}$formattedVolume"),
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textAssist,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = changeText,
                    fontSize = 14.sp,
                    color = changeColor,
                )
            }
        }
    }
}
