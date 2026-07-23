package one.mixin.android.ui.home.web3.market

import android.widget.ImageView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.AlertDialog
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.loadSvgWithTint
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormatCompact
import one.mixin.android.extension.priceFormat
import one.mixin.android.ui.home.web3.components.ActionButton
import one.mixin.android.ui.wallet.components.AssetDistribution
import one.mixin.android.ui.wallet.components.MultiColorProgressBar
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.market.GlobalMarket
import one.mixin.android.vo.market.MarketItem
import java.math.BigDecimal

@Composable
fun MarketPage(
    state: MarketPageUiState,
    showDisplaySettings: Boolean,
    onSearch: () -> Unit,
    onScan: () -> Unit,
    onShowDisplaySettings: () -> Unit,
    onDismissDisplaySettings: () -> Unit,
    onApplyDisplaySettings: (MarketDisplaySettings) -> Unit,
    onSelectTopTab: (MarketTopTab) -> Unit,
    onSelectSubTab: (MarketSubTab) -> Unit,
    onSort: (MarketSortColumn) -> Unit,
    onFavorite: (MarketListEntry) -> Unit,
    onToggleRecommendation: (String) -> Unit,
    onAddRecommendations: () -> Unit,
    onKeepPriceAlerts: () -> Unit,
    onDeletePriceAlerts: () -> Unit,
    onEntryClick: (MarketListEntry) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MixinAppTheme.colors.background),
    ) {
        MarketToolbar(
            onSearch = onSearch,
            onScan = onScan,
            onShowDisplaySettings = onShowDisplaySettings,
        )
        TopTabs(
            selected = state.selectedTopTab,
            onSelect = onSelectTopTab,
        )
        if (state.selectedTopTab != MarketTopTab.INDICATOR) {
            SubTabs(
                topTab = state.selectedTopTab,
                selected = state.selectedSubTab,
                onSelect = onSelectSubTab,
            )
            if (!state.showsPerpetualRecommendations) {
                MarketListHeader(
                    period = state.effectivePriceChangePeriod,
                    sortState = state.sortState,
                    onSort = onSort,
                    onShowDisplaySettings = onShowDisplaySettings,
                )
            }
            MarketList(
                state = state,
                onFavorite = onFavorite,
                onToggleRecommendation = onToggleRecommendation,
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

    if (state.pendingAlertCoinId != null) {
        AlertDialog(
            onDismissRequest = onKeepPriceAlerts,
            text = {
                Text(
                    text = stringResource(R.string.watchlist_remove_alert_prompt),
                    color = MixinAppTheme.colors.textPrimary,
                    textAlign = TextAlign.Center,
                )
            },
            dismissButton = {
                TextButton(onClick = onKeepPriceAlerts) {
                    Text(stringResource(R.string.Keep))
                }
            },
            confirmButton = {
                TextButton(onClick = onDeletePriceAlerts) {
                    Text(stringResource(R.string.Delete))
                }
            },
            backgroundColor = MixinAppTheme.colors.background,
        )
    }
}

@Composable
private fun MarketToolbar(
    onSearch: () -> Unit,
    onScan: () -> Unit,
    onShowDisplaySettings: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.Markets),
            color = MixinAppTheme.colors.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onSearch) {
            Icon(
                painter = painterResource(R.drawable.ic_search_home),
                contentDescription = null,
                tint = MixinAppTheme.colors.icon,
            )
        }
        IconButton(onClick = onScan) {
            Icon(
                painter = painterResource(R.drawable.ic_bot_category_scan),
                contentDescription = null,
                tint = Color.Unspecified,
            )
        }
        IconButton(onClick = onShowDisplaySettings) {
            Icon(
                painter = painterResource(R.drawable.ic_home_setting),
                contentDescription = stringResource(R.string.market_display),
                tint = Color.Unspecified,
            )
        }
    }
}

@Composable
private fun TopTabs(
    selected: MarketTopTab,
    onSelect: (MarketTopTab) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MarketTopTab.entries.forEach { tab ->
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
    val tabs =
        if (topTab == MarketTopTab.WATCHLIST) {
            listOf(MarketSubTab.CRYPTO, MarketSubTab.PERPETUAL)
        } else {
            listOf(
                MarketSubTab.TRENDING,
                MarketSubTab.TOP_GAINERS,
                MarketSubTab.TOP_LOSERS,
                MarketSubTab.ALL,
            )
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { tab ->
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
                        .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun MarketChip(
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
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun MarketListHeader(
    period: MarketPriceChangePeriod,
    sortState: MarketSortState,
    onSort: (MarketSortColumn) -> Unit,
    onShowDisplaySettings: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 20.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onShowDisplaySettings),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_config),
                contentDescription = stringResource(R.string.market_display),
                tint = Color.Unspecified,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        SortLabel(
            text = "Vol",
            column = MarketSortColumn.VOLUME,
            sortState = sortState,
            onSort = onSort,
        )
        Spacer(modifier = Modifier.weight(1f))
        SortLabel(
            text = stringResource(R.string.Price),
            column = MarketSortColumn.PRICE,
            sortState = sortState,
            onSort = onSort,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier =
                Modifier
                    .width(1.dp)
                    .height(12.dp)
                    .background(MixinAppTheme.colors.backgroundWindow),
        )
        Spacer(modifier = Modifier.width(10.dp))
        SortLabel(
            text = priceChangePeriodLabel(period),
            column = MarketSortColumn.CHANGE,
            sortState = sortState,
            onSort = onSort,
        )
    }
}

@Composable
private fun SortLabel(
    text: String,
    column: MarketSortColumn,
    sortState: MarketSortState,
    modifier: Modifier = Modifier,
    onSort: (MarketSortColumn) -> Unit,
) {
    val icon =
        when {
            sortState.column != column -> R.drawable.ic_perps_sort_default
            sortState.direction == MarketSortDirection.ASCENDING -> R.drawable.ic_perps_sort_asc
            sortState.direction == MarketSortDirection.DESCENDING -> R.drawable.ic_perps_sort_desc
            else -> R.drawable.ic_perps_sort_default
        }
    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { onSort(column) }
                .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            color = MixinAppTheme.colors.textAssist,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun MarketList(
    state: MarketPageUiState,
    onFavorite: (MarketListEntry) -> Unit,
    onToggleRecommendation: (String) -> Unit,
    onAddRecommendations: () -> Unit,
    onEntryClick: (MarketListEntry) -> Unit,
) {
    when {
        state.isLoading && state.entries.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MixinAppTheme.colors.accent)
            }
        }

        state.showsPerpetualRecommendations -> {
            PerpetualRecommendations(
                recommendations = state.perpetualRecommendations,
                selectedIds = state.selectedRecommendationIds,
                isAdding = state.isAddingRecommendations,
                onToggle = onToggleRecommendation,
                onAdd = onAddRecommendations,
            )
        }

        state.entries.isEmpty() -> {
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
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(
                    items = state.entries,
                    key = MarketListEntry::stableId,
                ) { entry ->
                    MarketRow(
                        entry = entry,
                        settings = state.displaySettings,
                        onFavorite = { onFavorite(entry) },
                        onClick = { onEntryClick(entry) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PerpetualRecommendations(
    recommendations: List<MarketListEntry.Perpetual>,
    selectedIds: Set<String>,
    isAdding: Boolean,
    onToggle: (String) -> Unit,
    onAdd: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(
            items = recommendations.chunked(2),
            key = { row -> row.joinToString(separator = ":") { it.favoriteId } },
        ) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { entry ->
                    PerpetualRecommendationCard(
                        entry = entry,
                        selected = entry.favoriteId in selectedIds,
                        onClick = { onToggle(entry.favoriteId) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        item {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                ActionButton(
                    text = stringResource(R.string.Add_to_Watchlist),
                    onClick = onAdd,
                    backgroundColor = MixinAppTheme.colors.accent,
                    contentColor = Color.White,
                    enabled = selectedIds.isNotEmpty() && !isAdding,
                )
            }
        }
    }
}

@Composable
private fun PerpetualRecommendationCard(
    entry: MarketListEntry.Perpetual,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val change = entry.market.changePercentValue() ?: BigDecimal.ZERO
    val changeColor =
        if (change.signum() >= 0) {
            MixinAppTheme.colors.marketGreen
        } else {
            MixinAppTheme.colors.marketRed
        }
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MixinAppTheme.colors.background,
        border = BorderStroke(1.dp, MixinAppTheme.colors.borderColor),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MarketIcon(entry.market.iconUrl)
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.market.tokenSymbol,
                        color = MixinAppTheme.colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.Perpetual_Leverage_Format, entry.market.leverage),
                        color = MixinAppTheme.colors.textAssist,
                        fontSize = 10.sp,
                    )
                }
                Text(
                    text = formatPercent(change),
                    color = changeColor,
                    fontSize = 12.sp,
                )
            }
            Icon(
                painter =
                    painterResource(
                        if (selected) {
                            R.drawable.ic_asset_favorites_checked
                        } else {
                            R.drawable.ic_asset_favorites
                        },
                    ),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun MarketRow(
    entry: MarketListEntry,
    settings: MarketDisplaySettings,
    onFavorite: () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = 12.dp, end = 20.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onFavorite,
            enabled = true,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                painter =
                    painterResource(
                        if (entry.isFavored) {
                            R.drawable.ic_asset_favorites_checked
                        } else {
                            R.drawable.ic_asset_favorites
                        },
                    ),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        when (entry) {
            is MarketListEntry.Spot ->
                SpotMarketRowContent(
                    market = entry.market,
                    settings = settings,
                )

            is MarketListEntry.Perpetual ->
                PerpetualMarketRowContent(
                    market = entry.market,
                    settings = settings,
                )
        }
    }
}

@Composable
private fun RowScope.SpotMarketRowContent(
    market: MarketItem,
    settings: MarketDisplaySettings,
) {
    val change = market.changePercent(settings.priceChangePeriod)
    MarketIcon(url = market.iconUrl)
    Spacer(modifier = Modifier.width(10.dp))
    Column(modifier = Modifier.weight(1f)) {
        Text(
            text = market.symbol,
            color = MixinAppTheme.colors.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = stringResource(R.string.market_volume, formatSpotVolume(market.totalVolume)),
            color = MixinAppTheme.colors.textAssist,
            fontSize = 10.sp,
            maxLines = 1,
        )
    }
    Text(
        text = formatSpotPrice(market.currentPrice),
        color = MixinAppTheme.colors.textPrimary,
        fontSize = 12.sp,
        textAlign = TextAlign.End,
        maxLines = 1,
    )
    Spacer(modifier = Modifier.width(16.dp))
    ChangeColumn(
        change = change,
        sparkline = market.sparkline(settings.priceChangePeriod),
        quoteColorReversed = settings.quoteColorReversed,
    )
}

@Composable
private fun RowScope.PerpetualMarketRowContent(
    market: PerpsMarket,
    settings: MarketDisplaySettings,
) {
    val change = market.changePercentValue()
    MarketIcon(url = market.iconUrl)
    Spacer(modifier = Modifier.width(10.dp))
    Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = market.tokenSymbol,
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = stringResource(R.string.perps_badge),
                color = MixinAppTheme.colors.textAssist,
                fontSize = 10.sp,
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MixinAppTheme.colors.backgroundGrayLight)
                        .padding(horizontal = 3.dp, vertical = 1.dp),
            )
        }
        Text(
            text = stringResource(R.string.market_volume, formatUsdVolume(market.volume)),
            color = MixinAppTheme.colors.textAssist,
            fontSize = 10.sp,
            maxLines = 1,
        )
    }
    Text(
        text = "$${market.last}",
        color = MixinAppTheme.colors.textPrimary,
        fontSize = 12.sp,
        textAlign = TextAlign.End,
        maxLines = 1,
    )
    Spacer(modifier = Modifier.width(16.dp))
    ChangeColumn(
        change = change,
        sparkline = null,
        quoteColorReversed = settings.quoteColorReversed,
    )
}

@Composable
private fun MarketIcon(url: String) {
    CoilImage(
        model = url,
        placeholder = R.drawable.ic_avatar_place_holder,
        modifier =
            Modifier
                .size(34.dp)
                .clip(CircleShape),
    )
}

@Composable
private fun ChangeColumn(
    change: BigDecimal?,
    sparkline: String?,
    quoteColorReversed: Boolean,
) {
    val isRising = change?.let { it >= BigDecimal.ZERO } ?: true
    val changeColor =
        if (change == null) {
            MixinAppTheme.colors.textAssist
        } else if (isRising) {
            if (quoteColorReversed) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
        } else {
            if (quoteColorReversed) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
        }
    if (sparkline.isNullOrBlank()) {
        Box(
            modifier =
                Modifier
                    .width(60.dp)
                    .height(36.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = change?.let(::formatPercent) ?: "--",
                color = changeColor,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            )
        }
    } else {
        Column(
            modifier = Modifier.width(60.dp),
            horizontalAlignment = Alignment.End,
        ) {
            AndroidView(
                factory = { context ->
                    ImageView(context).apply {
                        scaleType = ImageView.ScaleType.FIT_XY
                    }
                },
                update = { imageView ->
                    imageView.loadSvgWithTint(sparkline, isRising, quoteColorReversed)
                },
                modifier =
                    Modifier
                        .width(54.dp)
                        .height(20.dp),
            )
            Text(
                text = change?.let(::formatPercent) ?: "--",
                color = changeColor,
                fontSize = 11.sp,
                textAlign = TextAlign.End,
            )
        }
    }
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
            fontSize = 12.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            color = MixinAppTheme.colors.textPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = change?.let(::formatPercent) ?: "--",
            color = changeColor,
            fontSize = 12.sp,
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
                text = "${stringResource(R.string.Bitcoin)} ${stringResource(R.string.Dominance)}",
                color = MixinAppTheme.colors.textAssist,
                fontSize = 12.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${dominance?.numberFormat2() ?: indicator.dominancePercentage}%",
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
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
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
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
                            modifier =
                                Modifier
                                    .size(32.dp)
                                    .background(MixinAppTheme.colors.backgroundGray, CircleShape),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(16.dp),
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
                        onSelect = { pending = pending.copy(quoteColorReversed = it) },
                    )
                    if (showPriceChange) {
                        Spacer(modifier = Modifier.height(8.dp))
                        DisplaySettingRow(
                            title = stringResource(R.string.Price_Change),
                            value = priceChangePeriodLabel(pending.priceChangePeriod),
                            options =
                                listOf(
                                    priceChangePeriodLabel(MarketPriceChangePeriod.TWENTY_FOUR_HOURS) to MarketPriceChangePeriod.TWENTY_FOUR_HOURS,
                                    priceChangePeriodLabel(MarketPriceChangePeriod.SEVEN_DAYS) to MarketPriceChangePeriod.SEVEN_DAYS,
                                ),
                            selectedOption = pending.priceChangePeriod,
                            onSelect = { pending = pending.copy(priceChangePeriod = it) },
                        )
                    }
                    Spacer(modifier = Modifier.height(42.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        ActionButton(
                            text = stringResource(R.string.Reset),
                            onClick = {
                                pending =
                                    MarketDisplaySettings(
                                        priceChangePeriod =
                                            if (showPriceChange) {
                                                MarketDisplaySettings().priceChangePeriod
                                            } else {
                                                current.priceChangePeriod
                                            },
                                    )
                            },
                            backgroundColor = MixinAppTheme.colors.backgroundGray,
                            contentColor = MixinAppTheme.colors.textPrimary,
                            modifier =
                                Modifier
                                    .width(120.dp)
                                    .height(44.dp),
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        ActionButton(
                            text = stringResource(R.string.Apply),
                            onClick = {
                                onApply(pending)
                                onDismiss()
                            },
                            backgroundColor = MixinAppTheme.colors.accent,
                            contentColor = Color.White,
                            modifier =
                                Modifier
                                    .width(120.dp)
                                    .height(44.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
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
                    fontSize = 12.sp,
                    textAlign = TextAlign.End,
                )
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_gray_right),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(16.dp),
                )
            }
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

@Composable
private fun topTabLabel(tab: MarketTopTab): String =
    stringResource(
        when (tab) {
            MarketTopTab.WATCHLIST -> R.string.Watchlist
            MarketTopTab.CRYPTO -> R.string.Crypto
            MarketTopTab.PERPETUAL -> R.string.Perpetual
            MarketTopTab.STOCK -> R.string.market_stock
            MarketTopTab.INDICATOR -> R.string.Indicator
        },
    )

@Composable
private fun subTabLabel(tab: MarketSubTab): String =
    stringResource(
        when (tab) {
            MarketSubTab.TRENDING -> R.string.Trending
            MarketSubTab.TOP_GAINERS -> R.string.top_gainers
            MarketSubTab.TOP_LOSERS -> R.string.top_losers
            MarketSubTab.ALL -> R.string.All
            MarketSubTab.CRYPTO -> R.string.Crypto
            MarketSubTab.PERPETUAL -> R.string.Perpetual
        },
    )

@Composable
private fun priceChangePeriodLabel(period: MarketPriceChangePeriod): String =
    when (period) {
        MarketPriceChangePeriod.TWENTY_FOUR_HOURS -> stringResource(R.string.change_percent_period_hour, 24)
        MarketPriceChangePeriod.SEVEN_DAYS -> stringResource(R.string.change_percent_period_day, 7)
    }

private fun formatPercent(change: BigDecimal): String {
    val prefix = if (change > BigDecimal.ZERO) "+" else ""
    return "$prefix${change.numberFormat2()}%"
}

private fun formatSpotPrice(value: String): String =
    runCatching {
        "${Fiats.getSymbol()}${BigDecimal(value).multiply(BigDecimal(Fiats.getRate())).priceFormat()}"
    }.getOrDefault(value)

private fun formatSpotVolume(value: String): String =
    runCatching {
        "${Fiats.getSymbol()}${BigDecimal(value).multiply(BigDecimal(Fiats.getRate())).numberFormatCompact()}"
    }.getOrDefault(value)

private fun formatUsdVolume(value: String): String =
    runCatching { "$${BigDecimal(value).numberFormatCompact()}" }.getOrDefault(value)
