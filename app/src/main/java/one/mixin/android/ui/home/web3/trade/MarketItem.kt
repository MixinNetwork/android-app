package one.mixin.android.ui.home.web3.trade

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.numberFormatCompact
import one.mixin.android.ui.wallet.alert.components.cardBackground
import java.math.BigDecimal

@Composable
fun MarketItem(
    market: PerpsMarket,
    onClick: () -> Unit = {}
) {
    val change = try {
        BigDecimal(market.change)
    } catch (e: Exception) {
        BigDecimal.ZERO
    }

    val isPositive = change >= BigDecimal.ZERO
    val changeColor = if (isPositive) Color(0xFF4CAF50) else Color(0xFFF44336)
    val changeText = "${if (isPositive) "+" else ""}${market.change}%"

    val formattedPrice = try {
        val price = BigDecimal(market.markPrice)
        if (price >= BigDecimal("1000")) {
            String.format("%.2f", price)
        } else if (price >= BigDecimal("1")) {
            String.format("%.4f", price)
        } else {
            String.format("%.6f", price)
        }
    } catch (e: Exception) {
        market.markPrice
    }

    val formattedVolume = try {
        BigDecimal(market.volume).numberFormatCompact()
    } catch (e: Exception) {
        market.volume
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
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
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = market.displaySymbol,
                        fontSize = 16.sp,
                        color = MixinAppTheme.colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${market.leverage}x",
                        fontSize = 12.sp,
                        color = MixinAppTheme.colors.textAssist,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .cardBackground(
                                MixinAppTheme.colors.backgroundGrayLight,
                                Color.Transparent
                            )
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.Vol, formattedVolume),
                    fontSize = 12.sp,
                    color = MixinAppTheme.colors.textAssist,
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "$$formattedPrice",
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
