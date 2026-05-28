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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsPosition
import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.colorAttr
import one.mixin.android.extension.defaultSharedPreferences
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

    val displaySymbol = position.tokenSymbol ?: stringResource(R.string.Unknown)
    val quantity = position.quantity
        .toBigDecimalOrNull()
        ?.abs()
        ?.stripTrailingZeros()
        ?.toPlainString()
        ?: position.quantity.removePrefix("-")
    val isLong = position.side.equals("long", true)
    val isOpening = position.state.equals(PerpsPosition.STATE_OPENING, true)
    val isAdding = position.state.equals(PerpsPosition.STATE_ADDING, true)
    val isPending = isOpening || isAdding
    val sideColor = if (isLong) {
        if (quoteColorPref) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
    } else {
        if (quoteColorPref) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
    }
    val leverageTextColor = if (isPending) MixinAppTheme.colors.textAssist else sideColor
    val leverageBackgroundColor = if (isPending) {
        MixinAppTheme.colors.backgroundGrayLight
    } else {
        sideColor.copy(alpha = 0.1f)
    }
    val hasTakeProfit = !position.takeProfitPrice.isNullOrBlank()
    val hasStopLoss = !position.stopLossPrice.isNullOrBlank()
    val tpSlTagText = when {
        hasTakeProfit && hasStopLoss -> stringResource(R.string.take_profit_stop_loss_label)
        hasTakeProfit -> stringResource(R.string.take_profit_label)
        hasStopLoss -> stringResource(R.string.stop_loss_label)
        else -> null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
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

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                        modifier = Modifier.weight(1f, fill = false)
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
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    )
                    if (tpSlTagText != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        TpSlStatusTag(text = tpSlTagText)
                    }
                }

                if (isPending) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(if (isAdding) R.string.adding_position else R.string.Pending),
                        fontSize = 14.sp,
                        color = MixinAppTheme.colors.textAssist,
                        textAlign = TextAlign.End,
                    )
                } else {
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicText(
                        text = formatPerpsUsdDecimal(margin),
                        modifier = Modifier.widthIn(max = 120.dp),
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = MixinAppTheme.colors.textPrimary,
                            textAlign = TextAlign.End
                        ),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        autoSize = TextAutoSize.StepBased(
                            minFontSize = 8.sp,
                            maxFontSize = 14.sp,
                            stepSize = 0.5.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$quantity ${position.tokenSymbol ?: ""}",
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textAssist,
                    modifier = Modifier.weight(1f)
                )

                if (isPending) {
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    val unrealizedPnl = position.unrealizedPnl?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val roe = (position.roe?.toBigDecimalOrNull() ?: BigDecimal.ZERO).multiply(BigDecimal(100))
                    val isProfit = unrealizedPnl >= BigDecimal.ZERO
                    val pnlColor = if (isProfit) {
                        if (quoteColorPref) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
                    } else {
                        if (quoteColorPref) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    BasicText(
                        text = "${formatPerpsSignedRawUsdDecimal(unrealizedPnl)} (${formatPerpsSignedPercent(roe, withSign = false)})",
                        modifier = Modifier.widthIn(max = 120.dp),
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = pnlColor,
                            textAlign = TextAlign.End
                        ),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        autoSize = TextAutoSize.StepBased(
                            minFontSize = 8.sp,
                            maxFontSize = 14.sp,
                            stepSize = 0.5.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun TpSlStatusTag(
    text: String,
) {
    val backgroundColor = Color(LocalContext.current.colorAttr(R.attr.bg_market_gradient_start))
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.W500,
        color = Color.White,
        lineHeight = 14.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 3.dp, vertical = 1.dp),
    )
}
