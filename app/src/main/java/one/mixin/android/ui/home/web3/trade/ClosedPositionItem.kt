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
import androidx.compose.foundation.layout.widthIn
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
import one.mixin.android.api.response.perps.PerpsOrderItem
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.ui.home.web3.trade.perps.formatPerpsSignedPercent
import one.mixin.android.ui.home.web3.trade.perps.formatPerpsSignedRawUsdDecimal
import java.math.BigDecimal

@Composable
fun ClosedPositionItem(
    order: PerpsOrderItem,
    onClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val quoteColorPref = context.defaultSharedPreferences
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
        if (quoteColorPref) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
    } else {
        if (quoteColorPref) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
    }
    val leverageBackgroundColor = sideColor.copy(alpha = 0.1f)
    val pnl = order.realizedPnl.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val isProfit = pnl >= BigDecimal.ZERO
    val pnlColor = if (isProfit) {
        if (quoteColorPref) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
    } else {
        if (quoteColorPref) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
    }
    val pnlPercent = order.roe.toBigDecimalOrNull()?.multiply(BigDecimal(100))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
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

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val sideText = if (isLong) {
                    stringResource(R.string.Long)
                } else {
                    stringResource(R.string.Short)
                }
                Text(
                    text = sideText,
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
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${order.leverage}x",
                    fontSize = 12.sp,
                    color = sideColor,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(leverageBackgroundColor)
                        .padding(horizontal = 3.dp, vertical = 2.dp),
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$quantity ${order.tokenSymbol ?: ""}",
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textAssist,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        BasicText(
            text = buildString {
                append(formatPerpsSignedRawUsdDecimal(pnl))
                pnlPercent?.let {
                    append(" (")
                    append(formatPerpsSignedPercent(it, withSign = false))
                    append(")")
                }
            },
            modifier = Modifier.widthIn(max = 120.dp),
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
    }
}
