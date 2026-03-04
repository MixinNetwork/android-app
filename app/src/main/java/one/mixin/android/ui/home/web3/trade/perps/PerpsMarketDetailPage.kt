package one.mixin.android.ui.home.web3.trade.perps

import PageScaffold
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.api.response.perps.PerpsPositionHistoryItem
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

@Composable
fun PerpsMarketDetailPage(
    marketId: String,
    marketSymbol: String,
    displaySymbol: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<PerpetualViewModel>()
    var market by remember { mutableStateOf<PerpsMarket?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTimeFrame by remember { mutableIntStateOf(0) }
    var currentPosition by remember { mutableStateOf<PerpsPositionItem?>(null) }
    var closedPositions by remember { mutableStateOf<List<PerpsPositionHistoryItem>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    val timeFrameValues = listOf("1h", "1d", "1w", "1M")
    val timeFrameLabels = listOf(
        stringResource(R.string.hours_count_short, 1),
        stringResource(R.string.days_count_short, 1),
        stringResource(R.string.weeks_count_short, 1),
        stringResource(R.string.months_count_short, 1),
    )
    val quoteColorReversed = context.defaultSharedPreferences
        .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    val risingColor = if (quoteColorReversed) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
    val fallingColor = if (quoteColorReversed) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed

    val walletId = Session.getAccountId() ?: ""

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

        if (walletId.isNotEmpty()) {
            viewModel.getPositionByMarket(walletId, marketId) { position ->
                currentPosition = position
            }

            viewModel.getClosedPositionsByMarket(walletId, marketId) { positions ->
                closedPositions = positions
            }
        }
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
                Spacer(modifier = Modifier.height(16.dp))

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
                            marketSymbol = marketSymbol,
                            displaySymbol = displaySymbol,
                            selectedTimeFrame = selectedTimeFrame,
                            timeFrameValues = timeFrameValues,
                            timeFrameLabels = timeFrameLabels,
                            onTimeFrameChange = { index ->
                                coroutineScope.launch { selectedTimeFrame = index }
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
                    OpenPositionCard(position = currentPosition!!)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (market != null) {
                    MarketInfoCard(
                        market = market!!,
                        onLearnClick = {
                            val activity = context as? FragmentActivity ?: return@MarketInfoCard
                            PerpetualGuideFragment.newInstance()
                                .show(activity.supportFragmentManager, PerpetualGuideFragment.TAG)
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
                                val position = currentPosition?.toPosition() ?: return@Button

                                PerpsCloseBottomSheetDialogFragment.newInstance(
                                    position = position,
                                ).setOnDone {
                                    currentPosition = null
                                }.show(activity.supportFragmentManager, PerpsCloseBottomSheetDialogFragment.TAG)
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
    val context = LocalContext.current
    val quoteColorReversed = context.defaultSharedPreferences
        .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    val risingColor = if (quoteColorReversed) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
    val fallingColor = if (quoteColorReversed) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
    val fiatRate = BigDecimal(Fiats.getRate())
    val fiatSymbol = Fiats.getSymbol()
    val fundingRate = market.fundingRate.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val fundingColor = when {
        fundingRate > BigDecimal.ZERO -> risingColor
        fundingRate < BigDecimal.ZERO -> fallingColor
        else -> MixinAppTheme.colors.textPrimary
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onLearnClick() },
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
        Column {
            Text(
                text = stringResource(R.string.Funding_Rate),
                fontSize = 12.sp,
                color = MixinAppTheme.colors.textAssist
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${market.fundingRate}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = fundingColor
            )
        }
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

@Composable
private fun MarketDetailCard(
    market: PerpsMarket,
    marketSymbol: String,
    displaySymbol: String,
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
                    text = displaySymbol,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MixinAppTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${fiatSymbol}$formattedPrice",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MixinAppTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
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
        ) {
            CandleChart(
                symbol = marketSymbol,
                timeFrame = timeFrameValues[selectedTimeFrame]
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            timeFrameLabels.forEachIndexed { index, timeFrameLabel ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .then(
                            if (selectedTimeFrame == index) {
                                Modifier.background(MixinAppTheme.colors.backgroundWindow)
                            } else {
                                Modifier
                            }
                        )
                        .clickable { onTimeFrameChange(index) },
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
    val markPrice = position.markPrice?.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val orderValue = quantity.multiply(markPrice).multiply(fiatRate)

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
                Row {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(directionColor)
                            .padding(horizontal = 8.dp, vertical = 0.5.dp)
                    ) {
                        Text(
                            text = if (isLong) stringResource(R.string.Long) else stringResource(R.string.Short),
                            fontSize = 10.sp,
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
                        text = stringResource(R.string.Order_Value),
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
                                PerpetualGuideFragment.newInstance()
                                    .show(activity.supportFragmentManager, PerpetualGuideFragment.TAG)
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
                        text = stringResource(R.string.Amount),
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
                                PerpetualGuideFragment.newInstance()
                                    .show(activity.supportFragmentManager, PerpetualGuideFragment.TAG)
                            },
                        tint = MixinAppTheme.colors.textAssist
                    )
                }
                Text(
                    text = "${fiatSymbol}${orderValue.priceFormat()}",
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
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_tip),
                        contentDescription = null,
                        modifier = Modifier
                            .size(12.dp)
                            .clickable {
                                val activity = context as? FragmentActivity ?: return@clickable
                                PerpetualGuideFragment.newInstance()
                                    .show(activity.supportFragmentManager, PerpetualGuideFragment.TAG)
                            },
                        tint = MixinAppTheme.colors.textAssist
                    )
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
                text = stringResource(R.string.Closed_Positions),
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
