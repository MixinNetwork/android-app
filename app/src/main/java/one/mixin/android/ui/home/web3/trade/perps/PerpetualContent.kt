package one.mixin.android.ui.home.web3.trade.perps

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.api.response.perps.PerpsPositionHistoryItem
import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putString
import one.mixin.android.session.Session
import one.mixin.android.ui.home.web3.trade.ClosedPositionItem
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.vo.Fiats
import one.mixin.android.widget.components.MixinButton
import java.math.BigDecimal
import java.math.RoundingMode

private const val POSITION_REFRESH_INTERVAL_MS = 3_000L
private const val CLOSED_POSITION_PREVIEW_LIMIT = 10

@Composable
fun PerpetualContent(
    onShowTradingGuide: () -> Unit,
    onShowMarketList: (isLong: Boolean) -> Unit,
    onShowAllMarkets: (String?) -> Unit,
    onShowAllOpenPositions: () -> Unit,
    onShowAllClosedPositions: () -> Unit,
    onOpenPositionClick: (PerpsPositionItem) -> Unit,
    onMarketItemClick: (PerpsMarket) -> Unit,
    onClosedPositionClick: (PerpsPositionHistoryItem) -> Unit,
) {
    val context = LocalContext.current
    val walletId = Session.getAccountId()!!
    val quoteColorReversed = context.defaultSharedPreferences
        .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    val risingColor = if (quoteColorReversed) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
    val fallingColor = if (quoteColorReversed) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
    val fiatSymbol = Fiats.getSymbol()
    val fiatRate = BigDecimal(Fiats.getRate())
    val viewModel = hiltViewModel<PerpetualViewModel>()
    val lifecycleOwner = LocalLifecycleOwner.current

    val markets by remember {
        viewModel.observeMarkets()
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val openPositions by remember(walletId) {
        viewModel.observeOpenPositions(walletId)
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val totalPnl by remember(walletId) {
        viewModel.observeTotalUnrealizedPnl(walletId)
    }.collectAsStateWithLifecycle(initialValue = 0.0)
    val closedPositions by remember(walletId) {
        viewModel.observeClosedPositions(walletId, CLOSED_POSITION_PREVIEW_LIMIT)
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    var previousOpenPositionsCount by remember(walletId) { mutableStateOf<Int?>(null) }
    val openPositionsCount = openPositions.size
    val openPositionsPreview = openPositions.take(3)
    val marketsPreview = markets.take(3)
    val sourceOrder = remember(markets) {
        markets.withIndex().associate { it.value.marketId to it.index }
    }
    val stocksMarkets = markets
        .filter { it.isStocksCategory() }
        .sortedBy { sourceOrder[it.marketId] ?: Int.MAX_VALUE }
    val commoditiesMarkets = markets
        .filter { it.isCommoditiesCategory() }
        .sortedBy { sourceOrder[it.marketId] ?: Int.MAX_VALUE }
    val stocksMarketsPreview = stocksMarkets.take(3)
    val commoditiesMarketsPreview = commoditiesMarkets.take(3)
    val closedPositionsPreview = closedPositions.take(3)
    val totalMargin = openPositions.fold(BigDecimal.ZERO) { total, position ->
        total + (position.margin?.toBigDecimalOrNull() ?: BigDecimal.ZERO)
    }
    val totalPnlAmount = BigDecimal.valueOf(totalPnl)
    val totalPositionValueFiatText = "${fiatSymbol}${formatPerpsDisplayDecimal(totalMargin.multiply(fiatRate))}"
    val totalPnlFiatText = "${if (totalPnlAmount >= BigDecimal.ZERO) "+" else "-"}$fiatSymbol${formatPerpsDisplayDecimal(totalPnlAmount.abs().multiply(fiatRate))}"
    val totalPnlPercent = calculatePnlPercent(totalMargin, totalPnlAmount)

    LaunchedEffect(Unit) {
        viewModel.loadMarkets(
            onSuccess = {
                isLoading = false
            },
            onError = { error ->
                errorMessage = error
                isLoading = false
            }
        )
    }

    LaunchedEffect(walletId, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.loadAcceptedAssets(
                onSuccess = { assetIds ->
                    context.defaultSharedPreferences.putString(
                        Constants.Account.PREF_PERPS_ACCEPTED_ASSET_IDS_V2,
                        assetIds.joinToString(",")
                    )
                }
            )
            viewModel.refreshPositionHistory(walletId, limit = CLOSED_POSITION_PREVIEW_LIMIT)
            while (isActive) {
                viewModel.refreshMarkets(
                    onError = { error ->
                        errorMessage = error
                    }
                )
                viewModel.refreshPositions(walletId)
                delay(POSITION_REFRESH_INTERVAL_MS)
            }
        }
    }

    LaunchedEffect(walletId, openPositionsCount) {
        val lastCount = previousOpenPositionsCount
        if (lastCount != null && openPositionsCount < lastCount) {
            viewModel.refreshPositionHistory(walletId, limit = CLOSED_POSITION_PREVIEW_LIMIT)
        }
        previousOpenPositionsCount = openPositionsCount
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .cardBackground(Color.Transparent, MixinAppTheme.colors.borderColor)
                        .padding(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.Total_Position_Value),
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textAssist,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = totalPositionValueFiatText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.W600,
                    color = MixinAppTheme.colors.textPrimary,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = totalPnlFiatText,
                        fontSize = 14.sp,
                        color = if (totalPnl >= 0) risingColor else fallingColor,
                    )
                    Text(
                        text = "(${formatPerpsSignedPercent(totalPnlPercent, withSign = false)})",
                        fontSize = 14.sp,
                        color = if (totalPnl >= 0) risingColor else fallingColor,
                    )
                }
            }

            if (openPositionsCount == 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
                        .clickable { onShowTradingGuide() }
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
            } else {
                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .cardBackground(Color.Transparent, MixinAppTheme.colors.borderColor)
                            .padding(vertical = 16.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onShowAllOpenPositions)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.positions_count, openPositionsCount),
                            fontSize = 14.sp,
                            color = MixinAppTheme.colors.textPrimary,
                        )
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_right),
                            contentDescription = null,
                            tint = MixinAppTheme.colors.textAssist,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    openPositionsPreview.forEachIndexed { index, position ->
                        OpenPositionItem(
                            position = position,
                            onClick = {
                                val targetMarket = markets.firstOrNull { it.marketId == position.marketId }
                                if (targetMarket != null) {
                                    onMarketItemClick(targetMarket)
                                } else {
                                    onOpenPositionClick(position)
                                }
                            }
                        )
                        if (index != openPositionsPreview.lastIndex) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    if (openPositionsCount > openPositionsPreview.size) {
                        Spacer(modifier = Modifier.height(12.dp))
                        ViewAllAction(onClick = onShowAllOpenPositions)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .cardBackground(Color.Transparent, MixinAppTheme.colors.borderColor)
                    .padding(vertical = 16.dp)
            ) {
                if (marketsPreview.isNotEmpty()) {
                    MarketPreviewSection(
                        title = stringResource(R.string.perps_markets),
                        markets = marketsPreview,
                        totalCount = markets.size,
                        quoteColorReversed = quoteColorReversed,
                        onTitleClick = { onShowAllMarkets(null) },
                        onViewAllClick = { onShowAllMarkets(null) },
                        onMarketItemClick = onMarketItemClick,
                    )
                } else if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MixinAppTheme.colors.accent,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else if (errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = errorMessage ?: "Error loading markets",
                            fontSize = 14.sp,
                            color = MixinAppTheme.colors.red,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            if (stocksMarketsPreview.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .cardBackground(Color.Transparent, MixinAppTheme.colors.borderColor)
                        .padding(vertical = 16.dp)
                ) {
                    MarketPreviewSection(
                        title = stringResource(R.string.perps_category_stocks),
                        markets = stocksMarketsPreview,
                        totalCount = stocksMarkets.size,
                        quoteColorReversed = quoteColorReversed,
                        onTitleClick = {
                            onShowAllMarkets(PerpsMarketListBottomSheetDialogFragment.CATEGORY_STOCKS)
                        },
                        onViewAllClick = {
                            onShowAllMarkets(PerpsMarketListBottomSheetDialogFragment.CATEGORY_STOCKS)
                        },
                        onMarketItemClick = onMarketItemClick,
                    )
                }
            }

            if (commoditiesMarketsPreview.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .cardBackground(Color.Transparent, MixinAppTheme.colors.borderColor)
                        .padding(vertical = 16.dp)
                ) {
                    MarketPreviewSection(
                        title = stringResource(R.string.perps_category_commodities),
                        markets = commoditiesMarketsPreview,
                        totalCount = commoditiesMarkets.size,
                        quoteColorReversed = quoteColorReversed,
                        onTitleClick = {
                            onShowAllMarkets(PerpsMarketListBottomSheetDialogFragment.CATEGORY_COMMODITIES)
                        },
                        onViewAllClick = {
                            onShowAllMarkets(PerpsMarketListBottomSheetDialogFragment.CATEGORY_COMMODITIES)
                        },
                        onMarketItemClick = onMarketItemClick,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Closed position Section
            Column(
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .cardBackground(Color.Transparent, MixinAppTheme.colors.borderColor)
                    .padding(vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onShowAllClosedPositions)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.perps_activity),
                        fontSize = 14.sp,
                        color = MixinAppTheme.colors.textPrimary,
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_right),
                        contentDescription = null,
                        tint = MixinAppTheme.colors.textAssist,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (closedPositions.isEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_empty_transaction),
                            contentDescription = null,
                            tint = MixinAppTheme.colors.backgroundGrayLight,
                            modifier = Modifier.size(78.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.No_Activity),
                            fontSize = 14.sp,
                            color = MixinAppTheme.colors.textAssist,
                        )
                    }
                } else {
                    closedPositionsPreview.forEach { position ->
                        ClosedPositionItem(
                            position = position,
                            onClick = { onClosedPositionClick(position) })
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (closedPositions.size > closedPositionsPreview.size) {
                        ViewAllAction(onClick = onShowAllClosedPositions)
                    }
                }
            }

            if (openPositionsCount > 0) {
                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
                        .clickable { onShowTradingGuide() }
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
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MixinButton(
                onClick = {
                    onShowMarketList(true)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                backgroundColor = risingColor,
                contentColor = Color.White,
            ) {
                Text(
                    fontSize = 16.sp,
                    text = stringResource(R.string.Long),
                )
            }

            MixinButton(
                onClick = {
                    onShowMarketList(false)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                backgroundColor = fallingColor,
                contentColor = Color.White,
                enabled = markets.isNotEmpty(),
            ) {
                Text(
                    fontSize = 16.sp,
                    text = stringResource(R.string.Short),
                )
            }
        }
    }
}

@Composable
private fun MarketPreviewSection(
    title: String,
    markets: List<PerpsMarket>,
    totalCount: Int,
    quoteColorReversed: Boolean,
    onTitleClick: () -> Unit,
    onViewAllClick: () -> Unit,
    onMarketItemClick: (PerpsMarket) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTitleClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            color = MixinAppTheme.colors.textPrimary,
        )
        Icon(
            painter = painterResource(R.drawable.ic_arrow_right),
            contentDescription = null,
            tint = MixinAppTheme.colors.textAssist,
            modifier = Modifier.size(16.dp),
        )
    }
    Spacer(modifier = Modifier.height(12.dp))

    markets.forEachIndexed { index, market ->
        PerpsMarketItem(
            market = market,
            quoteColorReversed = quoteColorReversed,
            onClick = { onMarketItemClick(market) }
        )
        if (index != markets.lastIndex) {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (totalCount > markets.size) {
        Spacer(modifier = Modifier.height(12.dp))
        ViewAllAction(onClick = onViewAllClick)
    }
}

private fun PerpsMarket.isStocksCategory(): Boolean {
    return category.equals("stock", ignoreCase = true) || category.equals("stocks", ignoreCase = true)
}

private fun PerpsMarket.isCommoditiesCategory(): Boolean {
    return category.equals("commodity", ignoreCase = true) || category.equals("commodities", ignoreCase = true)
}

private fun calculatePnlPercent(
    margin: BigDecimal,
    pnl: BigDecimal,
): Double {
    if (margin <= BigDecimal.ZERO) {
        return 0.0
    }
    return pnl
        .divide(margin, 8, RoundingMode.HALF_UP)
        .multiply(BigDecimal(100))
        .toDouble()
}


@Composable
private fun ViewAllAction(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.view_all),
            fontSize = 14.sp,
            color = MixinAppTheme.colors.accent,
            fontWeight = FontWeight.W500,
        )
    }
}
