package one.mixin.android.ui.home.web3.trade.perps

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.isActive
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.api.response.perps.PerpsPositionHistoryItem
import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.api.response.perps.toPosition
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.numberFormatCompact
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.priceFormat
import one.mixin.android.session.Session
import one.mixin.android.ui.home.web3.components.PageScaffold
import one.mixin.android.ui.home.web3.trade.CandleChart
import one.mixin.android.ui.home.web3.trade.ClosedPositionItem
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.vo.Fiats
import one.mixin.android.widget.components.MixinButton
import java.math.BigDecimal

private const val CLOSED_POSITION_PREVIEW_LIMIT = 100
private const val MARKET_REFRESH_INTERVAL_MS = 10_000L

@Composable
fun PerpsMarketDetailPage(
    marketId: String,
    marketSymbol: String,
    displaySymbol: String,
    tokenSymbol: String,
    initialMarket: PerpsMarket? = null,
    onBack: () -> Unit,
    onSharePosition: (PerpsPositionItem) -> Unit,
) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<PerpetualViewModel>()
    val lifecycleOwner = LocalLifecycleOwner.current
    var market by remember(marketId, initialMarket) { mutableStateOf(initialMarket) }
    var isLoading by remember(marketId, initialMarket) { mutableStateOf(initialMarket == null) }
    var selectedTimeFrame by remember { mutableIntStateOf(0) }
    val walletId = Session.getAccountId().orEmpty()
    val openPositions by remember(walletId) {
        if (walletId.isNotEmpty()) {
            viewModel.observeOpenPositions(walletId)
        } else {
            flowOf(emptyList())
        }
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val allClosedPositions by remember(walletId) {
        if (walletId.isNotEmpty()) {
            viewModel.observeClosedPositions(walletId, CLOSED_POSITION_PREVIEW_LIMIT)
        } else {
            flowOf(emptyList())
        }
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    var previousOpenPositionsCount by remember(walletId) { mutableStateOf<Int?>(null) }
    val currentPosition = openPositions.firstOrNull { it.marketId == marketId }
    val closedPositions = allClosedPositions.filter { it.marketId == marketId }
    val timeFrameValues = listOf("1m", "5m", "15m", "1h", "4h", "1d", "1w")
    val timeFrameLabels = listOf(
        stringResource(R.string.minutes_count_short, 1),
        stringResource(R.string.minutes_count_short, 5),
        stringResource(R.string.minutes_count_short, 15),
        stringResource(R.string.hours_count_short, 1),
        stringResource(R.string.hours_count_short, 4),
        stringResource(R.string.days_count_short, 1),
        stringResource(R.string.weeks_count_short, 1),
    )
    val quoteColorReversed = context.defaultSharedPreferences
        .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    val risingColor = if (quoteColorReversed) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
    val fallingColor = if (quoteColorReversed) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed

    LaunchedEffect(marketId, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                viewModel.loadMarketDetail(
                    marketId = marketId,
                    onSuccess = { data ->
                        market = data
                        isLoading = false
                    },
                    onError = {
                        isLoading = false
                    }
                )
                delay(MARKET_REFRESH_INTERVAL_MS)
            }
        }
    }

    LaunchedEffect(walletId, openPositions.size) {
        if (walletId.isEmpty()) return@LaunchedEffect
        val lastCount = previousOpenPositionsCount
        val currentCount = openPositions.size
        if (lastCount != null && currentCount < lastCount) {
            viewModel.refreshPositionHistory(walletId, limit = CLOSED_POSITION_PREVIEW_LIMIT)
        }
        previousOpenPositionsCount = currentCount
    }

    PageScaffold(
        title = displaySymbol,
        subtitleText = stringResource(R.string.Perpetual),
        verticalScrollable = false,
        pop = onBack,
        actions = {
            IconButton(onClick = {
                context.openUrl(Constants.HelpLink.CUSTOMER_SERVICE)
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_support),
                    contentDescription = null,
                    tint = MixinAppTheme.colors.icon,
                )
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 80.dp)
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
                        .padding(16.dp)
                ) {
                    if (market != null) {
                        MarketDetailCard(
                            market = market!!,
                            marketId = marketId,
                            displaySymbol = displaySymbol,
                            tokenSymbol = tokenSymbol,
                            selectedTimeFrame = selectedTimeFrame,
                            timeFrameValues = timeFrameValues,
                            timeFrameLabels = timeFrameLabels,
                            onTimeFrameChange = { index ->
                                selectedTimeFrame = index
                            }
                        )
                    } else if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(340.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(40.dp),
                                color = MixinAppTheme.colors.accent
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (currentPosition != null && market != null) {
                    OpenPositionCard(
                        position = currentPosition,
                        viewModel = viewModel,
                        onShare = {
                            onSharePosition(currentPosition)
                        },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (market != null) {
                    MarketInfoCard(
                        market = market!!,
                        onLearnClick = {
                            val activity = context as? FragmentActivity ?: return@MarketInfoCard
                            PerpetualGuideBottomSheetDialogFragment.newInstance(
                                PerpetualGuideBottomSheetDialogFragment.TAB_OVERVIEW
                            ).show(activity.supportFragmentManager, PerpetualGuideBottomSheetDialogFragment.TAG)
                        }
                    )
                }

                if (closedPositions.isNotEmpty() && market != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    ClosedPositionsSection(
                        positions = closedPositions,
                        onViewAll = {
                            val activity = context as? FragmentActivity ?: return@ClosedPositionsSection
                            activity.supportFragmentManager
                                .beginTransaction()
                                .add(
                                    android.R.id.content,
                                    AllPositionsFragment.newClosedInstance(),
                                    AllPositionsFragment.TAG
                                )
                                .addToBackStack(null)
                                .commit()
                        },
                        onPositionClick = { position ->
                            val activity = context as? FragmentActivity ?: return@ClosedPositionsSection
                            activity.supportFragmentManager
                                .beginTransaction()
                                .setCustomAnimations(
                                    R.anim.slide_in_right,
                                    0,
                                    0,
                                    R.anim.slide_out_right
                                )
                                .add(
                                    android.R.id.content,
                                    PositionDetailFragment.newInstance(position),
                                    PositionDetailFragment.TAG
                                )
                                .addToBackStack(null)
                                .commit()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            if (market != null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(MixinAppTheme.colors.background)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 20.dp, top = 20.dp)
                ) {
                    if (currentPosition != null) {
                        MixinButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            enabled = currentPosition.state == "open",
                            onClick = {
                                val activity = context as? FragmentActivity ?: return@MixinButton
                                val position = currentPosition.toPosition()

                                PerpsCloseBottomSheetDialogFragment.newInstance(
                                    position = position,
                                ).show(activity.supportFragmentManager, PerpsCloseBottomSheetDialogFragment.TAG)
                            },
                            backgroundColor = if (currentPosition.state == "open") MixinAppTheme.colors.accent else MixinAppTheme.colors.backgroundWindow,
                            contentColor = if (currentPosition.state == "open") Color.White else MixinAppTheme.colors.textAssist,
                            shape = RoundedCornerShape(32.dp),
                        ) {
                            Text(
                                fontSize = 16.sp,
                                text = stringResource(if(currentPosition.state == "open") R.string.Close_Position else R.string.Opening),
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MixinButton(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                onClick = {
                                    PerpsActivity.showOpenPosition(
                                        context = context,
                                        marketId = marketId,
                                        marketSymbol = marketSymbol,
                                        marketDisplaySymbol = market?.displaySymbol ?: marketSymbol,
                                        marketTokenSymbol = market?.tokenSymbol ?: "",
                                        isLong = true
                                    )
                                },
                                backgroundColor = risingColor,
                                contentColor = Color.White,
                                shape = RoundedCornerShape(32.dp),
                            ) {
                                Text(
                                    fontSize = 16.sp,
                                    text = stringResource(R.string.Long),
                                )
                            }

                            MixinButton(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                onClick = {
                                    PerpsActivity.showOpenPosition(
                                        context = context,
                                        marketId = marketId,
                                        marketSymbol = marketSymbol,
                                        marketDisplaySymbol = market?.displaySymbol ?: marketSymbol,
                                        marketTokenSymbol = market?.tokenSymbol ?: "",
                                        isLong = false
                                    )
                                },
                                backgroundColor = fallingColor,
                                contentColor = Color.White,
                                shape = RoundedCornerShape(32.dp),
                            ) {
                                Text(
                                    fontSize = 16.sp,
                                    text = stringResource(R.string.Short),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarketInfoCard(
    market: PerpsMarket,
    onLearnClick: () -> Unit,
) {
    val fiatRate = BigDecimal(Fiats.getRate())
    val fiatSymbol = Fiats.getSymbol()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
            .clickable { onLearnClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(painter = painterResource(id = R.drawable.ic_perps_help), contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.how_perps_works),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MixinAppTheme.colors.textPrimary
                )
                Text(
                    text = stringResource(R.string.learn_how_to_trade_perps),
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textAssist
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.Volume_24H).uppercase(),
            fontSize = 14.sp,
            color = MixinAppTheme.colors.textAssist
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatVolume(market.volume, fiatRate, fiatSymbol),
            fontSize = 16.sp,
            color = MixinAppTheme.colors.textPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.Funding_Rate).uppercase(),
            fontSize = 14.sp,
            color = MixinAppTheme.colors.textAssist
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatFundingRate(market.fundingRate),
            fontSize = 16.sp,
            color = MixinAppTheme.colors.textPrimary
        )
    }
}

@Composable
private fun formatVolume(
    volume: String,
    fiatRate: BigDecimal,
    fiatSymbol: String,
): String {
    return try {
        val vol = BigDecimal(volume)
        vol.numberFormatCompact()
    } catch (e: NumberFormatException) {
        stringResource(R.string.N_A)
    }
}

private fun formatFundingRate(fundingRate: String): String {
    return runCatching {
        BigDecimal(fundingRate).multiply(BigDecimal(100)).stripTrailingZeros().toPlainString() + "%"
    }.getOrElse { fundingRate }
}

@Composable
private fun MarketDetailCard(
    market: PerpsMarket,
    marketId: String,
    displaySymbol: String,
    tokenSymbol: String,
    selectedTimeFrame: Int,
    timeFrameValues: List<String>,
    timeFrameLabels: List<String>,
    onTimeFrameChange: (Int) -> Unit,
) {
    val context = LocalContext.current
    val quoteColorReversed = context.defaultSharedPreferences
        .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    val risingColor = if (quoteColorReversed) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
    val fallingColor = if (quoteColorReversed) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed

    val changePercent = market.changePercent()
    val isPositive = changePercent >= BigDecimal.ZERO
    val changeColor = if (isPositive) risingColor else fallingColor
    val changeText = formatPerpsSignedPercent(changePercent)
    val displayTokenSymbol = tokenSymbol
        .takeIf { it.isNotBlank() }
        ?: market.tokenSymbol.takeIf { it.isNotBlank() }
        ?: displaySymbol

    val displayPrice = market.last

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayTokenSymbol,
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(7.dp))
                Text(
                    text = "$PERPS_USD_SYMBOL$displayPrice",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.W500,
                    color = MixinAppTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = changeText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = changeColor
                )
            }

            CoilImage(
                model = market.iconUrl,
                placeholder = R.drawable.ic_avatar_place_holder,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clipToBounds()
        ) {
            CandleChart(
                marketId = marketId,
                timeFrame = timeFrameValues[selectedTimeFrame],
                marketPrice = market.last
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            timeFrameLabels.forEachIndexed { index, timeFrameLabel ->
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .then(
                            if (selectedTimeFrame == index) {
                                Modifier.background(MixinAppTheme.colors.backgroundWindow)
                            } else {
                                Modifier
                            }
                        )
                        .clickable { onTimeFrameChange(index) }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = timeFrameLabel,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (selectedTimeFrame == index) {
                            MixinAppTheme.colors.textPrimary
                        } else {
                            MixinAppTheme.colors.textAssist
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun OpenPositionCard(
    position: PerpsPositionItem,
    viewModel: PerpetualViewModel,
    onShare: () -> Unit,
) {
    val context = LocalContext.current
    val quoteColorReversed = context.defaultSharedPreferences
        .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    val risingColor = if (quoteColorReversed) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
    val fallingColor = if (quoteColorReversed) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
    val fiatRate = BigDecimal(Fiats.getRate())
    val fiatSymbol = Fiats.getSymbol()

    val pnl = position.unrealizedPnl?.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val roe = (position.roe?.toBigDecimalOrNull() ?: BigDecimal.ZERO).multiply(BigDecimal(100))
    val isProfit = pnl >= BigDecimal.ZERO
    val pnlColor = if (isProfit) risingColor else fallingColor

    val isLong = position.side.equals("long", ignoreCase = true)
    val directionColor = if (isLong) risingColor else fallingColor

    val quantity = position.quantity.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val marginAmount = position.margin?.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val amountValue = marginAmount

    val entryPrice = position.entryPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val liquidationPrice = calculateLiquidationPriceValue(entryPrice, position.leverage, isLong)
    val compactTextStyle = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.perps_position),
                fontSize = 16.sp,
                lineHeight = 16.sp,
                style = compactTextStyle,
                fontWeight = FontWeight.Medium,
                color = MixinAppTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(id = R.drawable.ic_share_arrow),
                contentDescription = null,
                tint = MixinAppTheme.colors.accent,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onShare),
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.PnL).uppercase(),
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    style = compactTextStyle,
                    color = MixinAppTheme.colors.textAssist
                )
                Text(
                    text = stringResource(R.string.Direction).uppercase(),
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    style = compactTextStyle,
                    color = MixinAppTheme.colors.textAssist
                )
            }
            Spacer(modifier = Modifier.height(7.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${formatPerpsSignedRawUsdDecimal(pnl)} (${formatPerpsSignedPercent(roe, withSign = false)})",
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    style = compactTextStyle,
                    color = pnlColor
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(directionColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isLong) stringResource(R.string.Long) else stringResource(R.string.Short),
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            style = compactTextStyle,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${position.leverage}x",
                        fontSize = 14.sp,
                        lineHeight = 17.sp,
                        style = compactTextStyle,
                        color = MixinAppTheme.colors.textPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.position_size).uppercase(),
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        style = compactTextStyle,
                        color = MixinAppTheme.colors.textAssist
                    )
                    Spacer(modifier = Modifier.width(9.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_tip),
                        contentDescription = null,
                        modifier = Modifier
                            .size(12.dp)
                            .clickable {
                                val activity = context as? FragmentActivity ?: return@clickable
                                PerpetualGuideBottomSheetDialogFragment.newInstance(
                                    PerpetualGuideBottomSheetDialogFragment.TAB_POSITION
                                ).show(activity.supportFragmentManager, PerpetualGuideBottomSheetDialogFragment.TAG)
                            },
                        tint = MixinAppTheme.colors.textAssist
                    )
                }
                Text(
                    text = stringResource(R.string.Margin).uppercase(),
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    style = compactTextStyle,
                    color = MixinAppTheme.colors.textAssist
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${quantity.stripTrailingZeros().toPlainString()} ${position.tokenSymbol}",
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    style = compactTextStyle,
                    color = MixinAppTheme.colors.textPrimary
                )
                Text(
                    text = formatPerpsUsdDecimal(amountValue),
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    style = compactTextStyle,
                    color = MixinAppTheme.colors.textPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.Entry_Price).uppercase(),
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    style = compactTextStyle,
                    color = MixinAppTheme.colors.textAssist
                )
                Text(
                    text = stringResource(R.string.Liquidation_Price).uppercase(),
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    style = compactTextStyle,
                    color = MixinAppTheme.colors.textAssist
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${entryPrice.priceFormat()}",
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    style = compactTextStyle,
                    color = MixinAppTheme.colors.textPrimary
                )
                Text(
                    text = formatPerpsUsdDecimal(liquidationPrice),
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    style = compactTextStyle,
                    color = MixinAppTheme.colors.textPrimary
                )
            }
        }
    }
}

@Composable
private fun ClosedPositionsSection(
    positions: List<PerpsPositionHistoryItem>,
    onViewAll: () -> Unit,
    onPositionClick: (PerpsPositionHistoryItem) -> Unit,
) {
    val displayPositions = positions.take(3)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
            .padding(vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onViewAll)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.perps_activity),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MixinAppTheme.colors.textPrimary
            )

            Icon(
                painter = painterResource(R.drawable.ic_arrow_right),
                contentDescription = null,
                tint = MixinAppTheme.colors.textAssist,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        displayPositions.forEach { position ->
            ClosedPositionItem(
                position = position,
                onClick = { onPositionClick(position) }
            )
            if (position != displayPositions.last()) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

private fun calculateLiquidationPriceValue(
    entryPrice: BigDecimal,
    leverage: Int,
    isLong: Boolean,
): BigDecimal {
    if (entryPrice == BigDecimal.ZERO || leverage == 0) {
        return BigDecimal.ZERO
    }

    val liquidationPercent = BigDecimal(100.0 / leverage)
    val liquidationRatio = liquidationPercent.divide(BigDecimal(100), 8, java.math.RoundingMode.HALF_UP)

    return if (isLong) {
        entryPrice.multiply(BigDecimal.ONE.subtract(liquidationRatio))
    } else {
        entryPrice.multiply(BigDecimal.ONE.add(liquidationRatio))
    }
}
