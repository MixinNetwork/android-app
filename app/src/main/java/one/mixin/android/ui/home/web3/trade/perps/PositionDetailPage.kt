package one.mixin.android.ui.home.web3.trade.perps

import one.mixin.android.ui.home.web3.components.PageScaffold
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsPositionHistoryItem
import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.tip.wc.compose.ItemWalletContent
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.vo.Fiats
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun PositionDetailPage(
    position: PerpsPositionItem,
    quoteColorReversed: Boolean = false,
    pop: () -> Unit,
    onClose: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onSupport: (() -> Unit)? = null,
) {
    val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())
    
    fun formatDate(dateStr: String?): String {
        if (dateStr == null) return ""
        return try {
            val instant = Instant.parse(dateStr)
            instant.atZone(ZoneId.systemDefault()).format(dateFormat)
        } catch (e: Exception) {
            dateStr
        }
    }

    val risingColor = if (quoteColorReversed) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
    val fallingColor = if (quoteColorReversed) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
    val isLong = position.side.equals("long", ignoreCase = true)
    val sideColor = if (isLong) risingColor else fallingColor
    val sideText = if (isLong) {
        stringResource(R.string.Long)
    } else {
        stringResource(R.string.Short)
    }
    val title = if (isLong) {
        stringResource(R.string.Opened_Long)
    } else {
        stringResource(R.string.Opened_Short)
    }

    val quantity = position.quantity.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val absQuantity = quantity.abs()
    val entryPrice = position.entryPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val pnl = position.unrealizedPnl?.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val roe = (position.roe?.toBigDecimalOrNull() ?: BigDecimal.ZERO).multiply(BigDecimal(100))
    val pnlColor = if (pnl >= BigDecimal.ZERO) risingColor else fallingColor
    val liquidationPrice = calculateLiquidationPriceValue(entryPrice, position.leverage, isLong)
    val fiatRate = BigDecimal(Fiats.getRate())
    val fiatSymbol = Fiats.getSymbol()

    fun formatFiat(value: BigDecimal): String {
        return formatPerpsFiatDecimal(value.multiply(fiatRate), fiatSymbol)
    }

    fun formatSignedFiat(value: BigDecimal): String {
        return formatPerpsSignedFiatDecimal(value.multiply(fiatRate), fiatSymbol)
    }

    PageScaffold(
        title = title,
        verticalScrollable = false,
        pop = pop,
        actions = {
            IconButton(onClick = { onSupport?.invoke() }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_support),
                    contentDescription = null,
                    tint = MixinAppTheme.colors.icon,
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .cardBackground(
                        MixinAppTheme.colors.background,
                        MixinAppTheme.colors.borderColor
                    )
            ) {
                Spacer(modifier = Modifier.height(30.dp))
                
                CoilImage(
                    model = position.iconUrl,
                    placeholder = R.drawable.ic_avatar_place_holder,
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .align(Alignment.CenterHorizontally)
                )
                
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text = absQuantity.stripTrailingZeros().toPlainString(),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.W500,
                        fontFamily = FontFamily(Font(R.font.mixin_font)),
                        color = MixinAppTheme.colors.textPrimary,
                    )
                    val symbol = position.tokenSymbol?.takeIf { it.isNotBlank() }
                    if (symbol != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = symbol,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.W500,
                            color = MixinAppTheme.colors.textPrimary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(sideColor.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 2.5.dp)
                        .align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "$sideText ${position.leverage}x",
                        color = sideColor,
                        fontSize = 14.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MixinAppTheme.colors.backgroundWindow),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.Close_Position),
                        color = MixinAppTheme.colors.textPrimary,
                        fontWeight = FontWeight.W500,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onClose?.invoke() }
                            .padding(vertical = 10.dp),
                        textAlign = TextAlign.Center
                    )
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(24.dp)
                            .background(Color(0x0D000000))
                    )
                    Text(
                        text = stringResource(R.string.Share),
                        color = MixinAppTheme.colors.textPrimary,
                        fontWeight = FontWeight.W500,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onShare?.invoke() }
                            .padding(vertical = 10.dp),
                        textAlign = TextAlign.Center
                    )
                }
                
                Spacer(modifier = Modifier.height(30.dp))
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .wrapContentHeight()
                    .cardBackground(
                        MixinAppTheme.colors.background,
                        MixinAppTheme.colors.borderColor
                    )
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                PositionDetailItem(
                    label = stringResource(R.string.Perpetual).uppercase(),
                    value = position.displaySymbol ?: position.tokenSymbol ?: "Unknown",
                    icon = position.iconUrl
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                PositionDetailItem(
                    label = stringResource(R.string.PnL),
                    value = formatSignedFiat(pnl),
                    valueColor = pnlColor,
                    subtitle = formatSignedPercent(roe),
                )

                Spacer(modifier = Modifier.height(20.dp))

                PositionDetailItem(
                    label = stringResource(R.string.Entry_Price).uppercase(),
                    value = formatFiat(entryPrice)
                )

                Spacer(modifier = Modifier.height(20.dp))

                PositionDetailItem(
                    label = stringResource(R.string.Liquidation_Price).uppercase(),
                    value = formatFiat(liquidationPrice)
                )

                Spacer(modifier = Modifier.height(20.dp))

                ItemWalletContent(
                    title = stringResource(R.string.Wallet).uppercase(),
                    fontSize = 16.sp,
                    padding = 0.dp
                )

                Spacer(modifier = Modifier.height(20.dp))

                PositionDetailItem(
                    label = stringResource(R.string.Open_Time).uppercase(),
                    value = formatDate(position.createdAt)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

private fun calculateLiquidationPriceValue(
    entryPrice: BigDecimal,
    leverage: Int,
    isLong: Boolean,
): BigDecimal {
    if (entryPrice == BigDecimal.ZERO || leverage <= 0) {
        return BigDecimal.ZERO
    }

    val liquidationPercent = BigDecimal(100.0 / leverage)
    val liquidationRatio = liquidationPercent.divide(BigDecimal(100), 8, RoundingMode.HALF_UP)

    return if (isLong) {
        entryPrice.multiply(BigDecimal.ONE.subtract(liquidationRatio))
    } else {
        entryPrice.multiply(BigDecimal.ONE.add(liquidationRatio))
    }
}

@Composable
private fun PositionDetailItem(
    label: String,
    value: String,
    icon: String? = null,
    subtitle: String? = null,
    valueColor: Color = MixinAppTheme.colors.textPrimary,
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MixinAppTheme.colors.textAssist
        )
        Spacer(modifier = Modifier.height(4.dp))

        if (icon != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoilImage(
                    model = icon,
                    placeholder = R.drawable.ic_avatar_place_holder,
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = valueColor
                )
            }
        } else {
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = valueColor
            )
        }

        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = valueColor
            )
        }
    }
}


@Composable
fun PositionDetailPage(
    positionHistory: PerpsPositionHistoryItem,
    quoteColorReversed: Boolean = false,
    pop: () -> Unit,
    onTradeAgain: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onSupport: (() -> Unit)? = null,
) {
    val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())

    fun formatDate(dateStr: String?): String {
        if (dateStr == null) return ""
        return try {
            val instant = Instant.parse(dateStr)
            instant.atZone(ZoneId.systemDefault()).format(dateFormat)
        } catch (e: Exception) {
            dateStr
        }
    }

    val pnl = try {
        BigDecimal(positionHistory.realizedPnl)
    } catch (e: Exception) {
        BigDecimal.ZERO
    }

    val isProfit = pnl >= BigDecimal.ZERO
    val risingColor = if (quoteColorReversed) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
    val fallingColor = if (quoteColorReversed) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
    val pnlColor = if (isProfit) risingColor else fallingColor
    val isLong = positionHistory.side.equals("long", ignoreCase = true)
    val sideColor = if (isLong) risingColor else fallingColor

    val sideText = if (isLong) {
        stringResource(R.string.Long)
    } else {
        stringResource(R.string.Short)
    }
    val title = if (isLong) {
        stringResource(R.string.Closed_Long)
    } else {
        stringResource(R.string.Closed_Short)
    }

    val quantity = positionHistory.quantity.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val absQuantity = quantity.abs()
    val fiatRate = BigDecimal(Fiats.getRate())
    val fiatSymbol = Fiats.getSymbol()
    val roe = calculateClosedRoe(
        entryPrice = positionHistory.entryPrice,
        closePrice = positionHistory.closePrice,
        side = positionHistory.side,
        leverage = positionHistory.leverage,
    )

    fun formatFiat(value: BigDecimal): String {
        return formatPerpsFiatDecimal(value.multiply(fiatRate), fiatSymbol)
    }

    fun formatSignedFiat(value: BigDecimal): String {
        return formatPerpsSignedFiatDecimal(value.multiply(fiatRate), fiatSymbol)
    }

    PageScaffold(
        title = title,
        verticalScrollable = false,
        pop = pop,
        actions = {
            IconButton(onClick = { onSupport?.invoke() }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_support),
                    contentDescription = null,
                    tint = MixinAppTheme.colors.icon,
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .cardBackground(
                        MixinAppTheme.colors.background,
                        MixinAppTheme.colors.borderColor
                    )
            ) {
                Spacer(modifier = Modifier.height(30.dp))

                CoilImage(
                    model = positionHistory.iconUrl,
                    placeholder = R.drawable.ic_avatar_place_holder,
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text = absQuantity.stripTrailingZeros().toPlainString(),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.W500,
                        fontFamily = FontFamily(Font(R.font.mixin_font)),
                        color = MixinAppTheme.colors.textPrimary,
                    )
                    val symbol = positionHistory.tokenSymbol?.takeIf { it.isNotBlank() }
                    if (symbol != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = symbol,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.W500,
                            color = MixinAppTheme.colors.textPrimary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(sideColor.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 2.5.dp)
                        .align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "$sideText ${positionHistory.leverage}x",
                        color = sideColor,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MixinAppTheme.colors.backgroundWindow),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.Trade_Again),
                        color = MixinAppTheme.colors.textPrimary,
                        fontWeight = FontWeight.W500,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onTradeAgain?.invoke() }
                            .padding(vertical = 10.dp),
                        textAlign = TextAlign.Center
                    )
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(24.dp)
                            .background(Color(0x0D000000))
                    )
                    Text(
                        text = stringResource(R.string.Share),
                        color = MixinAppTheme.colors.textPrimary,
                        fontWeight = FontWeight.W500,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onShare?.invoke() }
                            .padding(vertical = 10.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .wrapContentHeight()
                    .cardBackground(
                        MixinAppTheme.colors.background,
                        MixinAppTheme.colors.borderColor
                    )
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                PositionDetailItem(
                    label = stringResource(R.string.Perpetual).uppercase(),
                    value = positionHistory.displaySymbol ?: positionHistory.tokenSymbol ?: "Unknown",
                    icon = positionHistory.iconUrl
                )

                Spacer(modifier = Modifier.height(20.dp))

                PositionDetailItem(
                    label = stringResource(R.string.PnL),
                    value = "${formatSignedFiat(pnl)}(${formatSignedPercent(roe)})",
                    valueColor = pnlColor,
                )

                Spacer(modifier = Modifier.height(20.dp))

                PositionDetailItem(
                    label = stringResource(R.string.Entry_Price).uppercase(),
                    value = formatFiat(positionHistory.entryPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO)
                )

                Spacer(modifier = Modifier.height(20.dp))

                PositionDetailItem(
                    label = stringResource(R.string.Close_Price).uppercase(),
                    value = formatFiat(positionHistory.closePrice.toBigDecimalOrNull() ?: BigDecimal.ZERO)
                )
                
                Spacer(modifier = Modifier.height(20.dp))

                ItemWalletContent(
                    title = stringResource(R.string.Wallet).uppercase(),
                    fontSize = 16.sp,
                    padding = 0.dp
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                PositionDetailItem(
                    label = stringResource(R.string.Close_Time).uppercase(),
                    value = formatDate(positionHistory.closedAt)
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

private fun formatSignedPercent(value: BigDecimal): String {
    val sign = when {
        value > BigDecimal.ZERO -> "+"
        value < BigDecimal.ZERO -> "-"
        else -> ""
    }
    val number = value.abs().setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    return "$sign$number%"
}
