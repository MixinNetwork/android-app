package one.mixin.android.ui.home.web3.trade.perps

import PageScaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.api.response.perps.PerpsPositionHistoryItem
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.priceFormat
import one.mixin.android.ui.tip.wc.compose.ItemWalletContent
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.vo.Fiats
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun PositionDetailPage(
    position: PerpsPositionItem,
    quoteColorReversed: Boolean = false,
    pop: () -> Unit,
    onClose: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onSupport: (() -> Unit)? = null,
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    fun formatDate(dateStr: String?): String {
        if (dateStr == null) return ""
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            val date = inputFormat.parse(dateStr)
            date?.let { dateFormat.format(it) } ?: dateStr
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
        stringResource(R.string.Perpetual_Opened_Long_Title)
    } else {
        stringResource(R.string.Perpetual_Opened_Short_Title)
    }

    val quantity = position.quantity.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val absQuantity = quantity.abs()
    val markPrice = position.markPrice?.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val orderValue = absQuantity * markPrice
    val fiatRate = BigDecimal(Fiats.getRate())
    val fiatSymbol = Fiats.getSymbol()

    fun formatFiat(value: BigDecimal): String {
        return "$fiatSymbol${value.multiply(fiatRate).priceFormat()}"
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

                Row(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(
                        text = "$sideText ",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.W500,
                        color = MixinAppTheme.colors.textPrimary,
                    )
                    Text(
                        text = position.tokenSymbol ?: "",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.W500,
                        color = MixinAppTheme.colors.textPrimary,
                    )
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(sideColor.copy(alpha = 0.2f))
                        .padding(horizontal = 3.dp, vertical = 2.dp)
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
                        text = stringResource(R.string.Perpetual_Guide_Close),
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
                    label = stringResource(R.string.Order_Value).uppercase(),
                    value = "${String.format("%f", absQuantity)} ${position.tokenSymbol ?: ""}",
                    subtitle = formatFiat(orderValue)
                )

                Spacer(modifier = Modifier.height(20.dp))

                PositionDetailItem(
                    label = stringResource(R.string.Entry_Price).uppercase(),
                    value = formatFiat(position.entryPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO)
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

@Composable
private fun PositionDetailItem(
    label: String,
    value: String,
    icon: String? = null,
    subtitle: String? = null
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
                    color = MixinAppTheme.colors.textPrimary
                )
            }
        } else {
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = MixinAppTheme.colors.textPrimary
            )
        }

        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textAssist
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
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun formatDate(dateStr: String?): String {
        if (dateStr == null) return ""
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            val date = inputFormat.parse(dateStr)
            date?.let { dateFormat.format(it) } ?: dateStr
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
        stringResource(R.string.Perpetual_Closed_Long_Title)
    } else {
        stringResource(R.string.Perpetual_Closed_Short_Title)
    }

    val quantity = positionHistory.quantity.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val absQuantity = quantity.abs()
    val closePrice = positionHistory.closePrice.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val orderValue = absQuantity * closePrice
    val fiatRate = BigDecimal(Fiats.getRate())
    val fiatSymbol = Fiats.getSymbol()

    fun formatFiat(value: BigDecimal): String {
        return "$fiatSymbol${value.multiply(fiatRate).priceFormat()}"
    }

    fun formatSignedFiat(value: BigDecimal): String {
        return when {
            value > BigDecimal.ZERO -> "+${formatFiat(value)}"
            value < BigDecimal.ZERO -> "-${formatFiat(value.abs())}"
            else -> formatFiat(BigDecimal.ZERO)
        }
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

                Text(
                    text = formatSignedFiat(pnl),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.W500,
                    color = pnlColor,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(sideColor.copy(alpha = 0.2f))
                        .padding(horizontal = 3.dp, vertical = 2.dp)
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
                    label = stringResource(R.string.Order_Value).uppercase(),
                    value = "${String.format("%f", absQuantity)} ${positionHistory.tokenSymbol ?: ""}",
                    subtitle = formatFiat(orderValue)
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
