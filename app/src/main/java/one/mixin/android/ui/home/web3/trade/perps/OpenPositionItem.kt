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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.vo.Fiats
import java.math.BigDecimal

@Composable
fun OpenPositionItem(
    position: PerpsPositionItem,
    onClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val quoteColorPref = context.defaultSharedPreferences
        .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    val margin = position.margin?.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val fiatRate = BigDecimal(Fiats.getRate())
    val fiatSymbol = Fiats.getSymbol()

    val displaySymbol = position.tokenSymbol ?: stringResource(R.string.Unknown)
    val quantity = formatPerpsDisplayDecimal(position.quantity.toBigDecimalOrNull())
    val isLong = position.side.equals("long", true)
    val isOpening = position.state.equals("opening", true)
    val sideColor = if (isLong) {
        if (quoteColorPref) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
    } else {
        if (quoteColorPref) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
    }
    val leverageTextColor = if (isOpening) MixinAppTheme.colors.textAssist else sideColor
    val leverageBackgroundColor = if (isOpening) {
        MixinAppTheme.colors.backgroundGrayLight
    } else {
        sideColor.copy(alpha = 0.1f)
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
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CoilImage(
                model = position.iconUrl,
                placeholder = R.drawable.ic_avatar_place_holder,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
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
                        color = MixinAppTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${position.leverage}x",
                        fontSize = 12.sp,
                        color = leverageTextColor,
                        lineHeight = 14.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(leverageBackgroundColor)
                            .padding(horizontal = 3.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$quantity ${position.tokenSymbol ?: ""}",
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textAssist
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            if (isOpening) {
                Text(
                    text = stringResource(R.string.Openning),
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textAssist
                )
            } else {
                val marginFiat = margin.multiply(fiatRate)
                Text(
                    text = "${fiatSymbol}${formatPerpsDisplayDecimal(marginFiat)}",
                    fontSize = 16.sp,
                    color = MixinAppTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                val unrealizedPnl = position.unrealizedPnl?.toBigDecimalOrNull()?: BigDecimal.ZERO
                val pnlFiat = unrealizedPnl.abs().multiply(fiatRate)
                val roe = position.roe?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                val isProfit = unrealizedPnl >= BigDecimal.ZERO
                val pnlColor = if (isProfit) {
                    if (quoteColorPref) {
                        MixinAppTheme.colors.walletRed
                    } else {
                        MixinAppTheme.colors.walletGreen
                    }
                } else {
                    if (quoteColorPref) {
                        MixinAppTheme.colors.walletGreen
                    } else {
                        MixinAppTheme.colors.walletRed
                    }
                }
                Text(
                    text = "${if (unrealizedPnl >= BigDecimal.ZERO) "+" else "-"}$fiatSymbol${formatPerpsDisplayDecimal(pnlFiat)}(${formatPerpsSignedPercent(roe)})",
                    fontSize = 14.sp,
                    color = pnlColor
                )
            }
        }
    }
}
