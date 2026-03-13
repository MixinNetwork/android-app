package one.mixin.android.ui.home.web3.trade.perps

import PageScaffold
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
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.flowOf
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.api.response.perps.PerpsPositionHistoryItem
import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.api.response.perps.toPosition
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.marketPriceFormat
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.priceFormat
import one.mixin.android.session.Session
import one.mixin.android.ui.home.web3.trade.CandleChart
import one.mixin.android.ui.home.web3.trade.ClosedPositionItem
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.vo.Fiats
import java.math.BigDecimal

private const val CLOSED_POSITION_PREVIEW_LIMIT = 100

@Composable
fun PerpsMarketDetailPage(
    marketId: String,
    marketSymbol: String,
    displaySymbol: String,
    tokenSymbol: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<PerpetualViewModel>()
    var market by remember { mutableStateOf<PerpsMarket?>(null) }
    var isLoading by remember { mutableStateOf(true) }
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

    LaunchedEffect(marketId) {
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
                                .height(400.dp),
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

                if (currentPosition != null) {
                    OpenPositionCard(
                        position = currentPosition,
                        viewModel = viewModel
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

                if (closedPositions.isNotEmpty()) {
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
                        .padding(bottom = 16.dp, top = 8.dp)
                ) {
                    if (currentPosition != null) {
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            onClick = {
                                val activity = context as? FragmentActivity ?: return@Button
                                val position = currentPosition.toPosition()

                                PerpsCloseBottomSheetDialogFragment.newInstance(
                                    position = position,
                                ).show(activity.supportFragmentManager, PerpsCloseBottomSheetDialogFragment.TAG)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                backgroundColor = MixinAppTheme.colors.accent
                            ),
                            shape = RoundedCornerShape(32.dp),
                            elevation = ButtonDefaults.elevation(
                                pressedElevation = 0.dp,
                                defaultElevation = 0.dp,
                                hoveredElevation = 0.dp,
                                focusedElevation = 0.dp
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.Close_Position),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
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
                                colors = ButtonDefaults.outlinedButtonColors(
                                    backgroundColor = risingColor
                                ),
                                shape = RoundedCornerShape(32.dp),
                                elevation = ButtonDefaults.elevation(
                                    pressedElevation = 0.dp,
                                    defaultElevation = 0.dp,
                                    hoveredElevation = 0.dp,
                                    focusedElevation = 0.dp
                                )
                            ) {
                                Text(
                                    text = stringResource(R.string.Long),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Button(
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
                                colors = ButtonDefaults.outlinedButtonColors(
                                    backgroundColor = fallingColor
                                ),
                                shape = RoundedCornerShape(32.dp),
                                elevation = ButtonDefaults.elevation(
                                    pressedElevation = 0.dp,
                                    defaultElevation = 0.dp,
                                    hoveredElevation = 0.dp,
                                    focusedElevation = 0.dp
                                )
                            ) {
                                Text(
                                    text = stringResource(R.string.Short),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
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
                    text = stringResource(R.string.How_Perps_Works),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MixinAppTheme.colors.textPrimary
                )
                Text(
                    text = stringResource(R.string.Learn_How_To_Trade_Perps),
                    fontSize = 12.sp,
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
            text = stringResource(R.string.Volume_24H),
            fontSize = 12.sp,
            color = MixinAppTheme.colors.textAssist
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatVolume(market.volume, fiatRate, fiatSymbol),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MixinAppTheme.colors.textPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.Funding_Rate),
            fontSize = 12.sp,
            color = MixinAppTheme.colors.textAssist
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatFundingRate(market.fundingRate),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MixinAppTheme.colors.textPrimary
        )
    }
}

private fun formatVolume(
    volume: String,
    fiatRate: BigDecimal,
    fiatSymbol: String,
): String {
    return try {
        val fiatVolume = BigDecimal(volume).multiply(fiatRate)
        "${fiatSymbol}${fiatVolume.priceFormat()}"
    } catch (e: Exception) {
        "${fiatSymbol}${volume}"
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
    val fiatRate = BigDecimal(Fiats.getRate())
    val fiatSymbol = Fiats.getSymbol()

    val change = try {
        BigDecimal(market.change)
    } catch (e: Exception) {
        BigDecimal.ZERO
    }

    val isPositive = change >= BigDecimal.ZERO
    val changeColor = if (isPositive) risingColor else fallingColor
    val changeText = "${if (isPositive) "+" else ""}${market.change}%"
    val displayTokenSymbol = tokenSymbol
        .takeIf { it.isNotBlank() }
        ?: market.tokenSymbol.takeIf { it.isNotBlank() }
        ?: displaySymbol

    val formattedPrice = try {
        val price = BigDecimal(market.markPrice).multiply(fiatRate)
        price.marketPriceFormat()
    } catch (e: Exception) {
        market.markPrice
    }

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
                    text = "${fiatSymbol}$formattedPrice",
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
                timeFrame = timeFrameValues[selectedTimeFrame]
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
) {
    val context = LocalContext.current
    val quoteColorReversed = context.defaultSharedPreferences
        .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    val risingColor = if (quoteColorReversed) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
    val fallingColor = if (quoteColorReversed) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
    val fiatRate = BigDecimal(Fiats.getRate())
    val fiatSymbol = Fiats.getSymbol()

    val pnl = position.unrealizedPnl?.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val isProfit = pnl >= BigDecimal.ZERO
    val pnlColor = if (isProfit) risingColor else fallingColor

    val isLong = position.side.equals("long", ignoreCase = true)
    val directionColor = if (isLong) risingColor else fallingColor

    val quantity = position.quantity.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val openPayToken by remember(position.openPayAssetId) {
        if (position.openPayAssetId.isNullOrEmpty()) {
            flowOf(null)
        } else {
            viewModel.observeTokenByAssetId(position.openPayAssetId)
        }
    }.collectAsStateWithLifecycle(initialValue = null)
    val openPayAmount = position.openPayAmount?.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val openPayPrice = openPayToken?.priceUsd?.toBigDecimalOrNull()
        ?: if (position.openPayAssetId in Constants.usdIds) BigDecimal.ONE else BigDecimal.ZERO
    val amountValue = openPayAmount.multiply(openPayPrice).multiply(fiatRate)

    val entryPrice = position.entryPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val liquidationPrice = calculateLiquidationPriceValue(entryPrice, position.leverage, isLong)

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
                fontWeight = FontWeight.Medium,
                color = MixinAppTheme.colors.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(R.string.PNL),
                    fontSize = 12.sp,
                    color = MixinAppTheme.colors.textAssist
                )
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${
                        if (isProfit) {
                            "+"
                        } else {
                            "-"
                        }
                    }${fiatSymbol}${pnl.abs().multiply(fiatRate).priceFormat()}",
                    fontSize = 14.sp,
                    color = pnlColor
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.Direction),
                    fontSize = 12.sp,
                    color = MixinAppTheme.colors.textAssist
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(directionColor)
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (isLong) stringResource(R.string.Long) else stringResource(R.string.Short),
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${position.leverage}x",
                        fontSize = 14.sp,
                        color = MixinAppTheme.colors.textPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.Position_Size),
                        fontSize = 12.sp,
                        color = MixinAppTheme.colors.textAssist
                    )
                    Spacer(modifier = Modifier.width(4.dp))
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
                    text = "${quantity.stripTrailingZeros().toPlainString()} ${position.tokenSymbol}",
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textPrimary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.Margin),
                        fontSize = 12.sp,
                        color = MixinAppTheme.colors.textAssist
                    )

//                    Spacer(modifier = Modifier.width(4.dp))
//                    Icon(
//                        painter = painterResource(id = R.drawable.ic_tip),
//                        contentDescription = null,
//                        modifier = Modifier
//                            .size(12.dp)
//                            .clickable {
//                                val activity = context as? FragmentActivity ?: return@clickable
//                                PerpetualGuideBottomSheetDialogFragment.newInstance()
//                                    .show(activity.supportFragmentManager, PerpetualGuideBottomSheetDialogFragment.TAG)
//                            },
//                        tint = MixinAppTheme.colors.textAssist
//                    )
                }
                Text(
                    text = "${fiatSymbol}${amountValue.priceFormat()}",
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(R.string.Entry_Price),
                    fontSize = 12.sp,
                    color = MixinAppTheme.colors.textAssist
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${fiatSymbol}${entryPrice.multiply(fiatRate).priceFormat()}",
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textPrimary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.Liquidation_Price),
                        fontSize = 12.sp,
                        color = MixinAppTheme.colors.textAssist
                    )
//                    Spacer(modifier = Modifier.width(4.dp))
//                    Icon(
//                        painter = painterResource(id = R.drawable.ic_tip),
//                        contentDescription = null,
//                        modifier = Modifier
//                            .size(12.dp)
//                            .clickable {
//                                val activity = context as? FragmentActivity ?: return@clickable
//                                PerpetualGuideBottomSheetDialogFragment.newInstance()
//                                    .show(activity.supportFragmentManager, PerpetualGuideBottomSheetDialogFragment.TAG)
//                            },
//                        tint = MixinAppTheme.colors.textAssist
//                    )
                }
                Text(
                    text = "${fiatSymbol}${liquidationPrice.multiply(fiatRate).priceFormat()}",
                    fontSize = 14.sp,
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
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.perps_activity),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MixinAppTheme.colors.textPrimary
            )

            if (positions.size > 3) {
                Text(
                    text = stringResource(R.string.view_all),
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.accent,
                    modifier = Modifier.clickable { onViewAll() }
                )
            }
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
