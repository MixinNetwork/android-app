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
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsPositionHistoryItem
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.vo.Fiats
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun ClosedPositionItem(
    position: PerpsPositionHistoryItem,
    onClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val quoteColorPref = context.defaultSharedPreferences
        .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    val fiatRate = BigDecimal(Fiats.getRate())
    val fiatSymbol = Fiats.getSymbol()
    
    val pnl = try {
        BigDecimal(position.realizedPnl)
    } catch (e: Exception) {
        BigDecimal.ZERO
    }

    val isProfit = pnl >= BigDecimal.ZERO
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
    
    val displaySymbol = position.tokenSymbol ?: "Unknown"
    val quantity = formatDisplayDecimal(position.quantity.toBigDecimalOrNull()?.abs())
    val pnlPercent = calculateClosedPercent(
        entryPrice = position.entryPrice,
        closePrice = position.closePrice,
        side = position.side,
        leverage = position.leverage,
    )

    val isLong = position.side.equals("long", true)
    val sideColor = if (isLong) {
        if (quoteColorPref) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
    } else {
        if (quoteColorPref) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
    }
    val leverageBackgroundColor = sideColor.copy(alpha = 0.1f)

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
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val sideText = if (position.side.lowercase() == "long") {
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
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${position.leverage}x",
                        fontSize = 12.sp,
                        color = sideColor,
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

        Column(
            horizontalAlignment = Alignment.End
        ) {
            val pnlFiat = pnl.abs().multiply(fiatRate)
            Text(
                text = "${if (isProfit) "+" else "-"}$fiatSymbol${formatDisplayDecimal(pnlFiat)}",
                fontSize = 16.sp,
                color = pnlColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatSignedPercent(pnlPercent),
                fontSize = 12.sp,
                color = pnlColor
            )
        }
    }
}

private fun calculateClosedPercent(
    entryPrice: String?,
    closePrice: String?,
    side: String,
    leverage: Int,
): BigDecimal {
    val entry = entryPrice?.toBigDecimalOrNull() ?: return BigDecimal.ZERO
    val close = closePrice?.toBigDecimalOrNull() ?: return BigDecimal.ZERO
    if (entry <= BigDecimal.ZERO || leverage <= 0) {
        return BigDecimal.ZERO
    }

    val direction = if (side.equals("short", ignoreCase = true)) BigDecimal(-1) else BigDecimal.ONE
    return close
        .subtract(entry)
        .divide(entry, 8, RoundingMode.HALF_UP)
        .multiply(BigDecimal(leverage))
        .multiply(BigDecimal(100))
        .multiply(direction)
}

private fun formatDisplayDecimal(value: BigDecimal?): String {
    val safeValue = value ?: BigDecimal.ZERO
    val absValue = safeValue.abs()
    if (absValue > BigDecimal.ZERO && absValue < BigDecimal("0.01")) {
        return "<0.01"
    }
    return safeValue.setScale(2, RoundingMode.HALF_UP).toPlainString()
}

private fun formatSignedPercent(value: BigDecimal): String {
    val sign = when {
        value > BigDecimal.ZERO -> "+"
        value < BigDecimal.ZERO -> "-"
        else -> ""
    }
    return "$sign${formatDisplayDecimal(value.abs())}%"
}
