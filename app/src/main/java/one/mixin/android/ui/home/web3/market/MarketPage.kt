package one.mixin.android.ui.home.web3.market

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormatCompact
import one.mixin.android.extension.priceFormat
import one.mixin.android.ui.wallet.components.AssetDistribution
import one.mixin.android.ui.wallet.components.MultiColorProgressBar
import one.mixin.android.widget.components.MixinButton
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.market.GlobalMarket
import one.mixin.android.vo.market.MarketItem
import one.mixin.android.widget.HomeToolbarView
import java.math.BigDecimal

@Composable
fun MarketPage(
    state: MarketPageUiState,
    showDisplaySettings: Boolean,
    onSearch: () -> Unit,
    onScan: () -> Unit,
    onShowSettings: () -> Unit,
    onShowDisplaySettings: () -> Unit,
    onDismissDisplaySettings: () -> Unit,
    onApplyDisplaySettings: (MarketDisplaySettings) -> Unit,
    onSelectTopTab: (MarketTopTab) -> Unit,
    onSelectSubTab: (MarketSubTab) -> Unit,
    onSort: (MarketSortColumn) -> Unit,
    onFavorite: (MarketListEntry, (Boolean) -> Unit) -> Unit,
    onAddRecommendations: (List<MarketListEntry>) -> Unit,
    onEntryClick: (MarketListEntry) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MixinAppTheme.colors.background),
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        MarketToolbar(
            onSearch = onSearch,
            onScan = onScan,
            onShowSettings = onShowSettings,
        )
        TopTabs(
            selected = state.selectedTopTab,
            onSelect = onSelectTopTab,
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (state.selectedTopTab != MarketTopTab.INDICATOR) {
            SubTabs(
                topTab = state.selectedTopTab,
                selected = state.selectedSubTab,
                onSelect = onSelectSubTab,
            )
            if (!state.isShowingRecommendations && state.entries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                MarketListHeader(
                    period = state.effectivePriceChangePeriod,
                    showMarketCap = state.showsMarketCapColumn,
                    sortState = state.sortState,
                    onSort = onSort,
                    onShowDisplaySettings = onShowDisplaySettings,
                )
            }
            MarketList(
                state = state,
                onFavorite = onFavorite,
                onAddRecommendations = onAddRecommendations,
                onEntryClick = onEntryClick,
            )
        } else {
            IndicatorPage(
                indicator = state.indicator,
                quoteColorReversed = state.displaySettings.quoteColorReversed,
                isLoading = state.isLoading,
            )
        }
    }

    if (showDisplaySettings) {
        MarketDisplayDialog(
            current = state.displaySettings,
            showPriceChange = !state.showsOnlyPerpetualMarkets,
            onDismiss = onDismissDisplaySettings,
            onApply = onApplyDisplaySettings,
        )
    }

}

@Composable
private fun MarketToolbar(
    onSearch: () -> Unit,
    onScan: () -> Unit,
    onShowSettings: () -> Unit,
) {
    val title = stringResource(R.string.Markets)
    AndroidView(
        factory = { context ->
            HomeToolbarView(context).apply {
                setTitle(title)
                setOnSearchClickListener { onSearch() }
                setOnScanClickListener { onScan() }
                setOnSettingsClickListener { onShowSettings() }
            }
        },
        update = { toolbar ->
            toolbar.setTitle(title)
            toolbar.setOnSearchClickListener { onSearch() }
            toolbar.setOnScanClickListener { onScan() }
            toolbar.setOnSettingsClickListener { onShowSettings() }
        },
        modifier =
            Modifier
                .fillMaxWidth()
                .height(56.dp),
    )
}

@Composable
private fun TopTabs(
    selected: MarketTopTab,
    onSelect: (MarketTopTab) -> Unit,
) {
    val tabs = MarketTopTab.entries
    val listState = rememberLazyListState()
    LaunchedEffect(selected) {
        listState.animateScrollToCenteredItem(tabs.indexOf(selected))
    }
    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = tabs,
            key = { it.name },
        ) { tab ->
            MarketChip(
                text = topTabLabel(tab),
                selected = selected == tab,
                onClick = { onSelect(tab) },
            )
        }
    }
}

@Composable
private fun SubTabs(
    topTab: MarketTopTab,
    selected: MarketSubTab?,
    onSelect: (MarketSubTab) -> Unit,
) {
    val tabs = marketSubTabs(topTab)
    val listState = rememberLazyListState()
    LaunchedEffect(topTab, selected) {
        listState.animateScrollToCenteredItem(selected?.let(tabs::indexOf) ?: -1)
    }
    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 6.dp, end = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(
            items = tabs,
            key = { it.name },
        ) { tab ->
            if (tab == MarketSubTab.FAVORITE) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .clip(CircleShape)
                            .background(
                                if (selected == tab) {
                                    MixinAppTheme.colors.accent.copy(alpha = 0.10f)
                                } else {
                                    Color.Transparent
                                },
                            ).clickable { onSelect(tab) }
                            .padding(horizontal = 7.dp, vertical = 5.dp),
                ) {
                    Icon(
                        painter =
                            painterResource(R.drawable.ic_title_favorites),
                        contentDescription = stringResource(R.string.Watchlist),
                        tint =
                            if (selected == tab) {
                                MixinAppTheme.colors.accent
                            } else {
                                MixinAppTheme.colors.textAssist
                            },
                        modifier = Modifier.size(MarketListFavoriteIconSize),
                    )
                }
            } else {
                Text(
                    text = subTabLabel(tab),
                    color = if (selected == tab) MixinAppTheme.colors.accent else MixinAppTheme.colors.textAssist,
                    fontSize = 14.sp,
                    modifier =
                        Modifier
                            .clip(CircleShape)
                            .background(
                                if (selected == tab) {
                                    MixinAppTheme.colors.accent.copy(alpha = 0.10f)
                                } else {
                                    Color.Transparent
                                },
                            ).clickable { onSelect(tab) }
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
    }
}

private suspend fun LazyListState.animateScrollToCenteredItem(index: Int) {
    if (index < 0) return
    var itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    if (itemInfo == null) {
        animateScrollToItem(index)
        itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    }
    itemInfo ?: return
    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
    val itemCenter = itemInfo.offset + itemInfo.size / 2
    animateScrollBy((itemCenter - viewportCenter).toFloat())
}

@Composable
internal fun MarketChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        color =
            if (selected) {
                MixinAppTheme.colors.accent.copy(alpha = 0.10f)
            } else {
                MixinAppTheme.colors.background
            },
        contentColor = if (selected) MixinAppTheme.colors.accent else MixinAppTheme.colors.textMinor,
        shape = CircleShape,
        border =
            androidx.compose.foundation.BorderStroke(
                1.dp,
                if (selected) MixinAppTheme.colors.accent else MixinAppTheme.colors.borderPrimary,
            ),
        modifier =
            Modifier
                .clip(CircleShape)
                .clickable(onClick = onClick),
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun MarketListHeader(
    period: MarketPriceChangePeriod,
    showMarketCap: Boolean,
    sortState: MarketSortState,
    onSort: (MarketSortColumn) -> Unit,
    onShowDisplaySettings: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = MarketHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(24.dp)
                    .offset(x = -MarketLeadingIconInset)
                    .clip(CircleShape)
                    .clickable(onClick = onShowDisplaySettings),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_config),
                contentDescription = stringResource(R.string.market_display),
                tint = Color.Unspecified,
            )
        }
        MarketHeaderSortLabels(
            period = period,
            showMarketCap = showMarketCap,
            sortState = sortState,
            onSort = onSort,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MarketList(
    state: MarketPageUiState,
    onFavorite: (MarketListEntry, (Boolean) -> Unit) -> Unit,
    onAddRecommendations: (List<MarketListEntry>) -> Unit,
    onEntryClick: (MarketListEntry) -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.selectedTopTab, state.selectedSubTab, state.sortState) {
        listState.scrollToItem(0)
    }
    when {
        state.showsMarketLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MixinAppTheme.colors.accent)
            }
        }

        state.isShowingRecommendations -> {
            MarketRecommendations(
                entries = state.entries,
                isAdding = state.isAddingRecommendations,
                quoteColorReversed = state.displaySettings.quoteColorReversed,
                onAdd = onAddRecommendations,
            )
        }

        state.entries.isEmpty() && state.hasLoadedLocalData -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text =
                        stringResource(
                            when {
                                state.hasError -> R.string.Network_error
                                state.selectedTopTab == MarketTopTab.WATCHLIST -> R.string.watchlist_empty
                                else -> R.string.No_Markets
                            },
                        ),
                    color = MixinAppTheme.colors.textAssist,
                    fontSize = 14.sp,
                )
            }
        }

        else -> {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    items = state.entries,
                    key = MarketListEntry::stableId,
                ) { entry ->
                    MarketRow(
                        entry = entry,
                        settings = state.displaySettings,
                        showMarketCap = state.showsMarketCapColumn,
                        onFavorite = { onResult -> onFavorite(entry, onResult) },
                        onClick = { onEntryClick(entry) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MarketRecommendations(
    entries: List<MarketListEntry>,
    isAdding: Boolean,
    quoteColorReversed: Boolean,
    onAdd: (List<MarketListEntry>) -> Unit,
) {
    val recommendations = entries.take(8)
    val recommendationIds = recommendations.map(MarketListEntry::stableId)
    var selectedIds by remember(recommendationIds) { mutableStateOf(recommendationIds.toSet()) }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        gridItems(
            items = recommendations,
            key = MarketListEntry::stableId,
        ) { entry ->
            val selected = entry.stableId in selectedIds
            MarketRecommendationCard(
                entry = entry,
                selected = selected,
                quoteColorReversed = quoteColorReversed,
                perpetualBadgeStyle = PerpetualMarketBadgeStyle.PERPETUAL,
                onSelect = {
                    selectedIds =
                        if (selected) {
                            selectedIds - entry.stableId
                        } else {
                            selectedIds + entry.stableId
                        }
                },
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                MixinButton(
                    onClick = { onAdd(recommendations.filter { it.stableId in selectedIds }) },
                    backgroundColor = MixinAppTheme.colors.accent,
                    contentColor = Color.White,
                    enabled = selectedIds.isNotEmpty() && !isAdding,
                    disabledBackgroundColor = MixinAppTheme.colors.backgroundGray,
                    disabledContentColor = MixinAppTheme.colors.textAssist,
                    modifier =
                        Modifier
                            .wrapContentWidth()
                            .height(42.dp),
                ) {
                    Text(
                        text = stringResource(R.string.Add_to_Watchlist),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.W400,
                    )
                }
            }
        }
    }
}

@Composable
private fun MarketRow(
    entry: MarketListEntry,
    settings: MarketDisplaySettings,
    showMarketCap: Boolean,
    onFavorite: ((Boolean) -> Unit) -> Unit,
    onClick: () -> Unit,
) {
    when (entry) {
        is MarketListEntry.Spot ->
            MarketListRowFrame(
                stableId = entry.stableId,
                isFavored = entry.isFavored,
                onFavorite = onFavorite,
                onClick = onClick,
            ) {
                SpotMarketRowContent(
                    market = entry.market,
                    settings = settings,
                    showMarketCap = showMarketCap,
                )
            }

        is MarketListEntry.Perpetual ->
            PerpsMarketListItem(
                market = entry.market,
                isFavored = entry.isFavored,
                quoteColorReversed = settings.quoteColorReversed,
                badgeStyle = PerpetualMarketBadgeStyle.PERPETUAL,
                onFavorite = onFavorite,
                onClick = onClick,
            )
    }
}

@Composable
private fun RowScope.SpotMarketRowContent(
    market: MarketItem,
    settings: MarketDisplaySettings,
    showMarketCap: Boolean,
) {
    val fiatSymbol = Fiats.getSymbol()
    val fiatRate = Fiats.getRate()
    val change =
        remember(
            settings.priceChangePeriod,
            market.priceChangePercentage24H,
            market.priceChangePercentage7D,
        ) {
            market.changePercent(settings.priceChangePeriod)
        }
    val volumeText =
        remember(
            showMarketCap,
            market.marketCap,
            market.totalVolume,
            fiatSymbol,
            fiatRate,
        ) {
            formatSpotVolume(
                if (showMarketCap) market.marketCap else market.totalVolume,
                fiatSymbol,
                fiatRate,
            )
        }
    val priceText = remember(market.currentPrice, fiatSymbol, fiatRate) {
        formatSpotPrice(market.currentPrice, fiatSymbol, fiatRate)
    }
    MarketIcon(url = market.iconUrl, size = 38.dp)
    Spacer(modifier = Modifier.width(10.dp))
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = market.symbol,
            color = MixinAppTheme.colors.textPrimary,
            fontSize = 14.sp,
            lineHeight = 14.sp,
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = market.marketCapRank,
                color = MixinAppTheme.colors.textAssist,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                style =
                    TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                    ),
                modifier =
                    Modifier
                        .background(
                            MixinAppTheme.colors.marketBadgeBackground,
                            RoundedCornerShape(4.dp),
                        )
                        .padding(horizontal = 4.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = volumeText,
                color = MixinAppTheme.colors.textAssist,
                fontSize = 12.sp,
                lineHeight = 12.sp,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                maxLines = 1,
            )
        }
    }
    MarketPriceText(text = priceText)
    Spacer(modifier = Modifier.width(MarketPriceChangeGap))
    MarketChangeColumn(
        change = change,
        sparkline = market.sparkline(settings.priceChangePeriod),
        quoteColorReversed = settings.quoteColorReversed,
        modifier = Modifier.width(MarketChangeColumnWidth),
    )
}

@Composable
private fun IndicatorPage(
    indicator: GlobalMarket?,
    quoteColorReversed: Boolean,
    isLoading: Boolean,
) {
    when {
        indicator == null && isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MixinAppTheme.colors.accent)
            }
        }

        indicator == null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.No_Markets),
                    color = MixinAppTheme.colors.textAssist,
                )
            }
        }

        else -> {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MarketOverviewCard(
                    indicator = indicator,
                    quoteColorReversed = quoteColorReversed,
                )
                BitcoinDominanceCard(indicator = indicator)
            }
        }
    }
}

@Composable
private fun MarketOverviewCard(
    indicator: GlobalMarket,
    quoteColorReversed: Boolean,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MixinAppTheme.colors.background,
        border = androidx.compose.foundation.BorderStroke(1.dp, MixinAppTheme.colors.borderPrimary),
        modifier =
            Modifier
                .fillMaxWidth()
                .height(126.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            IndicatorMetric(
                title = stringResource(R.string.Global_Market_Cap),
                value = formatSpotVolume(indicator.marketCap),
                change = indicator.marketCapChangePercentage.toBigDecimalOrNull(),
                quoteColorReversed = quoteColorReversed,
                modifier = Modifier.weight(1f),
            )
            IndicatorMetric(
                title = stringResource(R.string.volume_24h),
                value = formatSpotVolume(indicator.volume),
                change = indicator.volumeChangePercentage.toBigDecimalOrNull(),
                quoteColorReversed = quoteColorReversed,
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
            )
        }
    }
}

@Composable
private fun IndicatorMetric(
    title: String,
    value: String,
    change: BigDecimal?,
    quoteColorReversed: Boolean,
    modifier: Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
) {
    val changeColor =
        if (change == null) {
            MixinAppTheme.colors.textAssist
        } else if (change >= BigDecimal.ZERO) {
            if (quoteColorReversed) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
        } else {
            if (quoteColorReversed) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
        }
    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment,
    ) {
        Text(
            text = title,
            color = MixinAppTheme.colors.textAssist,
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            color = MixinAppTheme.colors.textPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.W500,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = change?.let(::formatPercent) ?: "--",
            color = changeColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.W500,
        )
    }
}

@Composable
private fun BitcoinDominanceCard(indicator: GlobalMarket) {
    val dominance =
        indicator.dominancePercentage
            .toBigDecimalOrNull()
            ?.coerceIn(BigDecimal.ZERO, BigDecimal(100))
    val btcPercentage = dominance?.divide(BigDecimal(100))?.toFloat() ?: 0f
    val otherPercentage = 1f - btcPercentage
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MixinAppTheme.colors.background,
        border = androidx.compose.foundation.BorderStroke(1.dp, MixinAppTheme.colors.borderPrimary),
        modifier =
            Modifier
                .fillMaxWidth()
                .height(150.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            Text(
                text = stringResource(R.string.Bitcoin_Dominance),
                color = MixinAppTheme.colors.textAssist,
                fontSize = 14.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${dominance?.numberFormat2() ?: indicator.dominancePercentage}%",
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.W500,
            )
            Spacer(modifier = Modifier.height(12.dp))
            MultiColorProgressBar(
                distributions =
                    listOf(
                        AssetDistribution(symbol = "BTC", percentage = btcPercentage, icons = emptyList()),
                        AssetDistribution(symbol = "OTHER", percentage = otherPercentage, icons = emptyList()),
                    ),
                segmentColors =
                    listOf(
                        MixinAppTheme.colors.walletYellow,
                        MixinAppTheme.colors.borderPrimary,
                    ),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                DominanceLegendItem(
                    label = "BTC",
                    percentage = btcPercentage,
                    color = MixinAppTheme.colors.walletYellow,
                    modifier = Modifier.weight(1f),
                )
                DominanceLegendItem(
                    label = stringResource(R.string.OTHER),
                    percentage = otherPercentage,
                    color = MixinAppTheme.colors.borderPrimary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DominanceLegendItem(
    label: String,
    percentage: Float,
    color: Color,
    modifier: Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(width = 3.dp, height = 9.dp)
                    .background(color, RoundedCornerShape(2.dp)),
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = "$label ${BigDecimal.valueOf(percentage.toDouble() * 100).numberFormat2()}%",
            color = MixinAppTheme.colors.textAssist,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun MarketDisplayDialog(
    current: MarketDisplaySettings,
    showPriceChange: Boolean,
    onDismiss: () -> Unit,
    onApply: (MarketDisplaySettings) -> Unit,
) {
    var pending by remember(current) { mutableStateOf(current) }
    fun applySettings(settings: MarketDisplaySettings) {
        pending = settings
        onApply(settings)
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss,
                        ),
            )
            Surface(
                color = MixinAppTheme.colors.backgroundWindow,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                modifier =
                    Modifier
                        .fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.market_display),
                            color = MixinAppTheme.colors.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_circle_close),
                                contentDescription = stringResource(R.string.Close),
                                tint = Color.Unspecified,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }
                    DisplaySettingRow(
                        title = stringResource(R.string.quote_color),
                        value =
                            stringResource(
                                if (pending.quoteColorReversed) {
                                    R.string.red_up_green_down
                                } else {
                                    R.string.green_up_red_down
                                },
                            ),
                        options =
                            listOf(
                                stringResource(R.string.green_up_red_down) to false,
                                stringResource(R.string.red_up_green_down) to true,
                            ),
                        selectedOption = pending.quoteColorReversed,
                        optionIcon = {
                            if (it) {
                                R.drawable.ic_queto_color_red
                            } else {
                                R.drawable.ic_queto_color_green
                            }
                        },
                        onSelect = {
                            applySettings(pending.copy(quoteColorReversed = it))
                        },
                    )
                    if (showPriceChange) {
                        Spacer(modifier = Modifier.height(8.dp))
                        DisplaySettingRow(
                            title = stringResource(R.string.Price_Change),
                            value = priceChangePeriodLabel(pending.priceChangePeriod),
                            options =
                                listOf(
                                    priceChangePeriodMenuLabel(MarketPriceChangePeriod.TWENTY_FOUR_HOURS) to MarketPriceChangePeriod.TWENTY_FOUR_HOURS,
                                    priceChangePeriodMenuLabel(MarketPriceChangePeriod.SEVEN_DAYS) to MarketPriceChangePeriod.SEVEN_DAYS,
                                ),
                            selectedOption = pending.priceChangePeriod,
                            onSelect = {
                                applySettings(pending.copy(priceChangePeriod = it))
                            },
                        )
                    }
                    Spacer(modifier = Modifier.height(120.dp))
                }
            }
        }
    }
}

@Composable
private fun <T> DisplaySettingRow(
    title: String,
    value: String,
    options: List<Pair<String, T>>,
    selectedOption: T,
    optionIcon: (T) -> Int? = { null },
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MixinAppTheme.colors.background)
                .clickable { expanded = true }
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = MixinAppTheme.colors.textPrimary,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier.width(180.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = value,
                    color = MixinAppTheme.colors.textAssist,
                    fontSize = 14.sp,
                    textAlign = TextAlign.End,
                )
                Box {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_gray_right),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(16.dp),
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        options.forEach { (label, option) ->
                            DropdownMenuItem(
                                onClick = {
                                    onSelect(option)
                                    expanded = false
                                },
                            ) {
                                if (option == selectedOption) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_check_black_24dp),
                                        contentDescription = null,
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(20.dp),
                                    )
                                } else {
                                    Spacer(modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = label,
                                    color = MixinAppTheme.colors.textPrimary,
                                    modifier = Modifier.weight(1f),
                                )
                                optionIcon(option)?.let { icon ->
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Icon(
                                        painter = painterResource(icon),
                                        contentDescription = null,
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun topTabLabel(tab: MarketTopTab): String =
    stringResource(
        when (tab) {
            MarketTopTab.WATCHLIST -> R.string.Watchlist
            MarketTopTab.CRYPTO -> R.string.Crypto
            MarketTopTab.PERPETUAL -> R.string.Perpetual
            MarketTopTab.INDICATOR -> R.string.Indicator
        },
    )

@Composable
private fun subTabLabel(tab: MarketSubTab): String =
    stringResource(
        when (tab) {
            MarketSubTab.FAVORITE -> R.string.Watchlist
            MarketSubTab.TRENDING -> R.string.Trending
            MarketSubTab.TOP_GAINERS -> R.string.top_gainers
            MarketSubTab.TOP_LOSERS -> R.string.top_losers
            MarketSubTab.ALL -> R.string.All
            MarketSubTab.CRYPTO -> R.string.Crypto
            MarketSubTab.PERPETUAL -> R.string.Perpetual
            MarketSubTab.INDICES -> R.string.perps_category_indices
            MarketSubTab.COMMODITIES -> R.string.perps_category_commodities
            MarketSubTab.FOREX -> R.string.perps_category_forex
            MarketSubTab.MEME -> R.string.perps_category_meme
        },
    )

@Composable
private fun priceChangePeriodLabel(period: MarketPriceChangePeriod): String =
    when (period) {
        MarketPriceChangePeriod.TWENTY_FOUR_HOURS -> stringResource(R.string.hours_count_short, 24)
        MarketPriceChangePeriod.SEVEN_DAYS -> stringResource(R.string.days_count_short, 7)
    }

@Composable
private fun priceChangePeriodMenuLabel(period: MarketPriceChangePeriod): String =
    when (period) {
        MarketPriceChangePeriod.TWENTY_FOUR_HOURS -> stringResource(R.string.hours_count_long, 24)
        MarketPriceChangePeriod.SEVEN_DAYS -> stringResource(R.string.days_count_long, 7)
    }

private fun formatPercent(change: BigDecimal): String {
    val prefix = if (change > BigDecimal.ZERO) "+" else ""
    return "$prefix${change.numberFormat2()}%"
}

private fun formatSpotPrice(
    value: String,
    fiatSymbol: String = Fiats.getSymbol(),
    fiatRate: Double = Fiats.getRate(),
): String =
    runCatching {
        "$fiatSymbol${BigDecimal(value).multiply(BigDecimal(fiatRate)).priceFormat()}"
    }.getOrDefault(value)

private fun formatSpotVolume(
    value: String,
    fiatSymbol: String = Fiats.getSymbol(),
    fiatRate: Double = Fiats.getRate(),
): String =
    runCatching {
        "$fiatSymbol${BigDecimal(value).multiply(BigDecimal(fiatRate)).numberFormatCompact()}"
    }.getOrDefault(value)
