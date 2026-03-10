package one.mixin.android.ui.home.web3.trade.perps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
    val change = try {
        BigDecimal(market.change)
    } catch (e: Exception) {
        BigDecimal.ZERO
    }

    val isPositive = change >= BigDecimal.ZERO
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
    val changeText = "${if (isPositive) "+" else ""}${market.change}%"
    val fiatRate = BigDecimal(Fiats.getRate())
    val fiatSymbol = Fiats.getSymbol()

    val formattedPrice = try {
        BigDecimal(market.markPrice).multiply(fiatRate).priceFormat()
    } catch (e: Exception) {
        market.markPrice
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
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            CoilImage(
                model = market.iconUrl,
                placeholder = R.drawable.ic_avatar_place_holder,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = market.tokenSymbol,
                        fontSize = 14.sp,
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
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.Vol, "$fiatSymbol$formattedVolume"),
                    fontSize = 12.sp,
                    color = MixinAppTheme.colors.textAssist,
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "$fiatSymbol$formattedPrice",
                fontSize = 16.sp,
                color = MixinAppTheme.colors.textPrimary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = changeText,
                fontSize = 12.sp,
                color = changeColor,
            )
        }
    }
}
