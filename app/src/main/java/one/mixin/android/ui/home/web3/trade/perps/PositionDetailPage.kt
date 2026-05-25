package one.mixin.android.ui.home.web3.trade.perps

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsOrder
import one.mixin.android.api.response.perps.PerpsOrderItem
import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.toast
import one.mixin.android.ui.home.web3.components.PageScaffold
import one.mixin.android.ui.tip.wc.compose.ItemWalletContent
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.util.getMixinErrorStringByCode
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
    val context = LocalContext.current
    val preferences = remember(context) { context.defaultSharedPreferences }
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
    val liquidationPriceText = position.liquidationPrice
        ?.takeIf { it.isNotBlank() }
        ?.let { formatPerpsPrice(it, position.priceScale) }
        ?: "--"
    val fiatRate = BigDecimal(Fiats.getRate())
    val fiatSymbol = Fiats.getSymbol()
    val hasTakeProfit = !position.takeProfitPrice.isNullOrBlank()
    val hasStopLoss = !position.stopLossPrice.isNullOrBlank()
    var hideTakeProfitGuideUntil by remember(preferences) {
        mutableStateOf(preferences.getTpSlGuideHideUntil(TpSlGuideType.TAKE_PROFIT))
    }
    var hideStopLossGuideUntil by remember(preferences) {
        mutableStateOf(preferences.getTpSlGuideHideUntil(TpSlGuideType.STOP_LOSS))
    }
    var guideType by remember(position.positionId) {
        mutableStateOf(
            resolveTpSlGuideType(
                pnl = pnl,
                hasTakeProfit = hasTakeProfit,
                hasStopLoss = hasStopLoss,
                hideTakeProfitGuideUntil = hideTakeProfitGuideUntil,
                hideStopLossGuideUntil = hideStopLossGuideUntil,
                now = System.currentTimeMillis(),
            )
        )
    }
    val viewModel = hiltViewModel<PerpetualViewModel>()

    LaunchedEffect(hasTakeProfit, hasStopLoss, guideType) {
        if ((guideType == TpSlGuideType.TAKE_PROFIT && hasTakeProfit) ||
            (guideType == TpSlGuideType.STOP_LOSS && hasStopLoss)
        ) {
            guideType = null
        }
    }

    val isPending = position.state == "processing" || position.state == "adding"
    val leverageTextColor = if (isPending) MixinAppTheme.colors.textAssist else sideColor
    val leverageBackgroundColor = if (isPending) {
        MixinAppTheme.colors.backgroundGrayLight
    } else {
        sideColor.copy(alpha = 0.1f)
    }

    fun showTpSlBottomSheetFromGuide(mode: PerpsTpSlBottomSheetDialogFragment.Mode) {
        val activity = context as? FragmentActivity ?: return
        val existingPrice = when (mode) {
            PerpsTpSlBottomSheetDialogFragment.Mode.TAKE_PROFIT -> position.takeProfitPrice.orEmpty()
            PerpsTpSlBottomSheetDialogFragment.Mode.STOP_LOSS -> position.stopLossPrice.orEmpty()
        }
        val currentPrice = position.markPrice.orEmpty().ifBlank { position.entryPrice }
        val requestMarginAmount = position.margin ?: position.openPayAmount.orEmpty()
        PerpsTpSlBottomSheetDialogFragment.newInstance(
            mode = mode,
            price = existingPrice,
            currentPrice = currentPrice,
            isLong = position.side.equals("long", ignoreCase = true),
            marketIconUrl = position.iconUrl.orEmpty(),
            marketSymbol = position.displaySymbol ?: position.tokenSymbol.orEmpty(),
            marginAmount = requestMarginAmount,
            leverage = position.leverage,
            entryPrice = position.entryPrice,
            marketId = position.marketId,
            priceScale = position.priceScale,
            liquidationPrice = position.liquidationPrice,
        ).setOnApply { value ->
            val normalizedValue = value?.trim().orEmpty()
            if (normalizedValue == existingPrice) return@setOnApply
            if (normalizedValue.isEmpty() && existingPrice.isEmpty()) return@setOnApply
            val requestedValue = normalizedValue.ifEmpty { "" }
            // TP/SL update API treats null as "keep existing value" and empty string as "clear this side".
            viewModel.setPositionTpSl(
                positionId = position.positionId,
                takeProfitPrice = when (mode) {
                    PerpsTpSlBottomSheetDialogFragment.Mode.TAKE_PROFIT -> requestedValue
                    PerpsTpSlBottomSheetDialogFragment.Mode.STOP_LOSS -> null
                },
                stopLossPrice = when (mode) {
                    PerpsTpSlBottomSheetDialogFragment.Mode.TAKE_PROFIT -> null
                    PerpsTpSlBottomSheetDialogFragment.Mode.STOP_LOSS -> requestedValue
                },
                onSuccess = {},
                onError = { errorCode, errorMessage ->
                    toast(context.getMixinErrorStringByCode(errorCode, errorMessage))
                },
            )
        }.show(activity.supportFragmentManager, PerpsTpSlBottomSheetDialogFragment.TAG)
    }

    fun formatFiat(value: BigDecimal): String {
        return formatPerpsUsdDecimal(value)
    }

    fun formatSignedFiat(value: BigDecimal): String {
        return formatPerpsSignedRawUsdDecimal(value)
    }

    fun formatPriceUsd(value: BigDecimal): String {
        return formatPerpsUsdDecimal(value)
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
                        .background(leverageBackgroundColor)
                        .padding(horizontal = 8.dp, vertical = 2.5.dp)
                        .align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "$sideText ${position.leverage}x",
                        color = leverageTextColor,
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
                    if (isPending) {
                        Text(
                            text = stringResource(if (position.state == "adding") R.string.adding_position else R.string.Pending),
                            color = MixinAppTheme.colors.textAssist,
                            fontWeight = FontWeight.W500,
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 10.dp),
                            textAlign = TextAlign.Center
                        )
                    } else {
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

                guideType?.let { currentGuideType ->
                    Spacer(modifier = Modifier.height(16.dp))
                    PerpsTpSlGuideCard(
                        guideType = currentGuideType,
                        onClose = {
                            val until = preferences.hideTpSlGuide(currentGuideType)
                            if (currentGuideType == TpSlGuideType.TAKE_PROFIT) {
                                hideTakeProfitGuideUntil = until
                            } else {
                                hideStopLossGuideUntil = until
                            }
                            guideType = null
                        },
                        actionText = stringResource(
                            if (currentGuideType == TpSlGuideType.TAKE_PROFIT) R.string.Take_Profit else R.string.Stop_Loss
                        ),
                        onActionClick = {
                            showTpSlBottomSheetFromGuide(
                                if (currentGuideType == TpSlGuideType.TAKE_PROFIT) PerpsTpSlBottomSheetDialogFragment.Mode.TAKE_PROFIT
                                else PerpsTpSlBottomSheetDialogFragment.Mode.STOP_LOSS
                            )
                        },
                        layout = PerpsTpSlGuideCardLayout.DETAIL,
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                PositionDetailItem(
                    label = stringResource(R.string.Entry_Price).uppercase(),
                    value = formatPriceUsd(entryPrice)
                )

                Spacer(modifier = Modifier.height(20.dp))

                PositionDetailItem(
                    label = stringResource(R.string.Liquidation_Price).uppercase(),
                    value = liquidationPriceText
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
    closeOrder: PerpsOrderItem,
    leverage: Int?,
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
        BigDecimal(closeOrder.realizedPnl)
    } catch (e: Exception) {
        BigDecimal.ZERO
    }

    val isProfit = pnl >= BigDecimal.ZERO
    val risingColor = if (quoteColorReversed) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
    val fallingColor = if (quoteColorReversed) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
    val pnlColor = if (isProfit) risingColor else fallingColor
    val isLong = closeOrder.side.equals("long", ignoreCase = true)
    val sideColor = if (isLong) risingColor else fallingColor

    val sideText = if (isLong) {
        stringResource(R.string.Long)
    } else {
        stringResource(R.string.Short)
    }
    val isFailed = closeOrder.status == PerpsOrder.STATUS_REJECTED
    val title = if (isFailed) {
        stringResource(if (isLong) R.string.Closed_Long_Failed else R.string.Closed_Short_Failed)
    } else {
        stringResource(if (isLong) R.string.Closed_Long else R.string.Closed_Short)
    }
    val leverageDimmed = isFailed || closeOrder.status == PerpsOrder.STATUS_PROCESSING
    val leverageTextColor = if (leverageDimmed) MixinAppTheme.colors.textAssist else sideColor
    val leverageBackgroundColor = if (leverageDimmed) {
        MixinAppTheme.colors.backgroundGrayLight
    } else {
        sideColor.copy(alpha = 0.1f)
    }

    val quantity = closeOrder.quantity.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val absQuantity = quantity.abs()
    val fiatRate = BigDecimal(Fiats.getRate())
    val fiatSymbol = Fiats.getSymbol()
    val effectiveLeverage = leverage ?: closeOrder.leverage
    val roe = (closeOrder.roe.toBigDecimalOrNull() ?: BigDecimal.ZERO).multiply(BigDecimal(100))

    fun formatFiat(value: BigDecimal): String {
        return formatPerpsUsdDecimal(value)
    }

    fun formatSignedFiat(value: BigDecimal): String {
        return formatPerpsSignedRawUsdDecimal(value)
    }

    fun formatPriceUsd(value: BigDecimal): String {
        return formatPerpsUsdDecimal(value)
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
                    model = closeOrder.iconUrl,
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
                    val symbol = closeOrder.tokenSymbol?.takeIf { it.isNotBlank() }
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
                        .background(leverageBackgroundColor)
                        .padding(horizontal = 8.dp, vertical = 2.5.dp)
                        .align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "$sideText ${effectiveLeverage}x",
                        color = leverageTextColor,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (!isFailed) {
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
                } else {
                    Spacer(modifier = Modifier.height(10.dp))
                }
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
                    value = closeOrder.displaySymbol ?: closeOrder.tokenSymbol ?: "Unknown",
                    icon = closeOrder.iconUrl
                )

                Spacer(modifier = Modifier.height(20.dp))

                PositionDetailItem(
                    label = stringResource(R.string.PnL).uppercase(),
                    value = "${formatSignedFiat(pnl)} (${formatSignedPercent(roe)})",
                    valueColor = pnlColor,
                )

                Spacer(modifier = Modifier.height(20.dp))

                PositionDetailItem(
                    label = stringResource(R.string.Entry_Price).uppercase(),
                    value = formatPriceUsd(closeOrder.entryPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO)
                )

                Spacer(modifier = Modifier.height(20.dp))

                PositionDetailItem(
                    label = stringResource(R.string.Close_Price).uppercase(),
                    value = formatPriceUsd(closeOrder.closePrice.toBigDecimalOrNull() ?: BigDecimal.ZERO)
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
                    value = formatDate(closeOrder.updatedAt)
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

private fun formatSignedPercent(value: BigDecimal): String {
    val scaled = value.abs().setScale(2, RoundingMode.FLOOR)
    val number = if (scaled.compareTo(BigDecimal.ZERO) == 0) "0.0" else scaled.stripTrailingZeros().toPlainString()
    return "$number%"
}

@Composable
fun OpenedOrderDetailPage(
    openedOrder: PerpsOrderItem,
    quoteColorReversed: Boolean = false,
    pop: () -> Unit,
    onViewMarket: (() -> Unit)? = null,
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
    val isLong = openedOrder.side.equals("long", ignoreCase = true)
    val sideColor = if (isLong) risingColor else fallingColor
    val sideText = if (isLong) {
        stringResource(R.string.Long)
    } else {
        stringResource(R.string.Short)
    }
    val isIncrease = openedOrder.orderType == PerpsOrder.TYPE_INCREASE
    val isFailed = openedOrder.status == PerpsOrder.STATUS_REJECTED
    val title = when {
        isIncrease && isFailed ->
            stringResource(if (isLong) R.string.Added_Long_Failed else R.string.Added_Short_Failed)
        isIncrease ->
            stringResource(if (isLong) R.string.Added_Long else R.string.Added_Short)
        isFailed ->
            stringResource(if (isLong) R.string.Opened_Long_Failed else R.string.Opened_Short_Failed)
        else ->
            stringResource(if (isLong) R.string.Opened_Long else R.string.Opened_Short)
    }
    val leverageDimmed = isFailed || openedOrder.status == PerpsOrder.STATUS_PROCESSING
    val leverageTextColor = if (leverageDimmed) MixinAppTheme.colors.textAssist else sideColor
    val leverageBackgroundColor = if (leverageDimmed) {
        MixinAppTheme.colors.backgroundGrayLight
    } else {
        sideColor.copy(alpha = 0.1f)
    }

    val quantity = openedOrder.quantity.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val absQuantity = quantity.abs()
    val entryPrice = openedOrder.entryPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val leverage = openedOrder.leverage

    fun formatPriceUsd(value: BigDecimal): String {
        return formatPerpsUsdDecimal(value)
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
                    model = openedOrder.iconUrl,
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
                    val symbol = openedOrder.tokenSymbol?.takeIf { it.isNotBlank() }
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
                        .background(leverageBackgroundColor)
                        .padding(horizontal = 8.dp, vertical = 2.5.dp)
                        .align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "$sideText ${leverage}x",
                        color = leverageTextColor,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (!isFailed) {
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
                            text = stringResource(R.string.view_perps_market),
                            color = MixinAppTheme.colors.textPrimary,
                            fontWeight = FontWeight.W500,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onViewMarket?.invoke() }
                                .padding(vertical = 10.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(30.dp))
                } else {
                    Spacer(modifier = Modifier.height(10.dp))
                }
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
                    value = openedOrder.displaySymbol ?: openedOrder.tokenSymbol ?: "Unknown",
                    icon = openedOrder.iconUrl
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (!isFailed) {
                    PositionDetailItem(
                        label = stringResource(R.string.Entry_Price).uppercase(),
                        value = formatPriceUsd(entryPrice)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    val amountValue = if (leverage > 0) {
                        absQuantity.multiply(entryPrice)
                            .divide(BigDecimal(leverage), 8, RoundingMode.HALF_UP)
                    } else {
                        BigDecimal.ZERO
                    }
                    PositionDetailItem(
                        label = stringResource(R.string.Amount).uppercase(),
                        value = formatPerpsUsdDecimal(amountValue)
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                }

                ItemWalletContent(
                    title = stringResource(R.string.Wallet).uppercase(),
                    fontSize = 16.sp,
                    padding = 0.dp
                )

                Spacer(modifier = Modifier.height(20.dp))

                PositionDetailItem(
                    label = stringResource(R.string.Open_Time).uppercase(),
                    value = formatDate(openedOrder.createdAt)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
