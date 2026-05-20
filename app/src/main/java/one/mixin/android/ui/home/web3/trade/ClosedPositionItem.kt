package one.mixin.android.ui.home.web3.trade

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
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsOrder
import one.mixin.android.api.response.perps.PerpsOrderItem
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.ui.home.web3.trade.perps.formatPerpsSignedRawUsdDecimal
import one.mixin.android.ui.home.web3.trade.perps.formatPerpsUsdDecimal
import java.math.BigDecimal

@Composable
fun ClosedPositionItem(
    order: PerpsOrderItem,
    onClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val quoteColorReversed = context.defaultSharedPreferences
        .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)

    val displaySymbol = order.displaySymbol ?: order.tokenSymbol ?: "Unknown"
    val quantity = order.quantity
        .toBigDecimalOrNull()
        ?.abs()
        ?.stripTrailingZeros()
        ?.toPlainString()
        ?: order.quantity.removePrefix("-")

    val isLong = order.side.equals("long", ignoreCase = true)
    val sideColor = if (isLong) {
        if (quoteColorReversed) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
    } else {
        if (quoteColorReversed) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
    }

    val isClose = order.orderType == PerpsOrder.TYPE_CLOSE
    val rowModifier = Modifier
        .fillMaxWidth()
        .let { if (isClose) it.clickable(onClick = onClick) else it }
        .padding(horizontal = 16.dp, vertical = 8.dp)

    val title = when (order.orderType) {
        PerpsOrder.TYPE_OPEN -> stringResource(if (isLong) R.string.Opened_Long else R.string.Opened_Short)
        PerpsOrder.TYPE_INCREASE -> stringResource(R.string.Added)
        PerpsOrder.TYPE_CLOSE -> stringResource(if (isLong) R.string.Closed_Long else R.string.Closed_Short)
        else -> displaySymbol
    }

    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoilImage(
            model = order.iconUrl,
            placeholder = R.drawable.ic_avatar_place_holder,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape),
        )

        Spacer(modifier = Modifier.width(12.dp))

        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        color = MixinAppTheme.colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = displaySymbol,
                        fontSize = 16.sp,
                        color = MixinAppTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$quantity ${order.tokenSymbol ?: ""}",
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textAssist,
                )
            }
        }

        if (isClose) {
            val pnl = order.realizedPnl.toBigDecimalOrNull() ?: BigDecimal.ZERO
            val isProfit = pnl >= BigDecimal.ZERO
            val pnlColor = if (isProfit) {
                if (quoteColorReversed) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
            } else {
                if (quoteColorReversed) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
            }
            BasicText(
                text = formatPerpsSignedRawUsdDecimal(pnl),
                modifier = Modifier.weight(0.85f),
                style = TextStyle(
                    fontSize = 14.sp,
                    color = pnlColor,
                    textAlign = TextAlign.End,
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 8.sp,
                    maxFontSize = 14.sp,
                    stepSize = 0.5.sp,
                ),
            )
        } else {
            val displayPrice = order.price.takeIf { it.isNotBlank() } ?: order.entryPrice
            val priceBd = displayPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO
            Text(
                text = formatPerpsUsdDecimal(priceBd),
                fontSize = 14.sp,
                color = sideColor,
                textAlign = TextAlign.End,
            )
        }
    }
}
