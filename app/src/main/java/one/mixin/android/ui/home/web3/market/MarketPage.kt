package one.mixin.android.ui.home.web3.market

import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.MixinAlertDialog
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.loadSvgWithTint
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormatCompact
import one.mixin.android.extension.priceFormat
import one.mixin.android.ui.wallet.components.AssetDistribution
import one.mixin.android.ui.wallet.components.MultiColorProgressBar
import one.mixin.android.widget.components.MixinButton
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
    onAddRecommendations: (List<MarketListEntry>) -> Unit,
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
        Spacer(modifier = Modifier.height(20.dp))
        if (state.selectedTopTab != MarketTopTab.INDICATOR) {
            SubTabs(
                topTab = state.selectedTopTab,
                selected = state.selectedSubTab,
                onSelect = onSelectSubTab,
            )
            if (!state.isShowingRecommendations && state.entries.isNotEmpty()) {
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

    if (state.pendingAlertCoinId != null) {
        MixinAlertDialog(
            onDismissRequest = onKeepPriceAlerts,
            onConfirmClick = onDeletePriceAlerts,
            onDismissClick = onKeepPriceAlerts,
            confirmText = stringResource(R.string.Delete),
            dismissText = stringResource(R.string.Keep),
            text = {
                Text(
                    text = stringResource(R.string.watchlist_remove_alert_prompt),
                    color = MixinAppTheme.colors.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
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
                contentDescription = stringResource(R.string.Search),
                tint = MixinAppTheme.colors.icon,
            )
        }
        IconButton(onClick = onScan) {
            Icon(
                painter = painterResource(R.drawable.ic_bot_category_scan),
                contentDescription = stringResource(R.string.Scan),
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
                .padding(horizontal = 12.dp),
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
    val tabs = marketSubTabs(topTab)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(start = 14.dp, end = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { tab ->
            if (tab == MarketSubTab.FAVORITE) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected == tab) {
                                    MixinAppTheme.colors.accent.copy(alpha = 0.10f)
                                } else {
                                    Color.Transparent
                                },
                            ).clickable { onSelect(tab) },
                ) {
                    Icon(
                        painter =
                            painterResource(
                                if (selected == tab) {
                                    R.drawable.ic_market_favorites_checked
                                } else {
                                    R.drawable.ic_market_favorites
                                },
                            ),
                        contentDescription = stringResource(R.string.Watchlist),
                        tint = Color.Unspecified,
                        modifier = Modifier.size(16.dp),
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
                .padding(start = 16.dp, end = 20.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            contentAlignment = Alignment.CenterStart,
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
        Spacer(modifier = Modifier.width(10.dp))
        SortLabel(
            text =
                stringResource(
                    if (showMarketCap) {
                        R.string.Market_Cap
                    } else {
                        R.string.market_volume_short
                    },
                ),
            column = MarketSortColumn.VOLUME,
            sortState = sortState,
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Start,
            onSort = onSort,
        )
        SortLabel(
            text = stringResource(R.string.Price),
            column = MarketSortColumn.PRICE,
            sortState = sortState,
            modifier = Modifier.width(96.dp),
            horizontalArrangement = Arrangement.End,
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
            modifier = Modifier.width(84.dp).offset(x = 4.dp),
            horizontalArrangement = Arrangement.End,
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
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
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
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            color = MixinAppTheme.colors.textAssist,
            fontSize = 12.sp,
            textAlign = if (horizontalArrangement == Arrangement.End) TextAlign.End else TextAlign.Start,
            maxLines = 1,
            softWrap = false,
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
    onAddRecommendations: (List<MarketListEntry>) -> Unit,
    onEntryClick: (MarketListEntry) -> Unit,
) {
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
                        showMarketCap = state.showsMarketCapColumn,
                        onFavorite = { onFavorite(entry) },
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
            RecommendationCard(
                entry = entry,
                selected = selected,
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
private fun RecommendationCard(
    entry: MarketListEntry,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    val symbol =
        when (entry) {
            is MarketListEntry.Spot -> entry.market.symbol
            is MarketListEntry.Perpetual -> entry.market.tokenSymbol
        }
    val iconUrl =
        when (entry) {
            is MarketListEntry.Spot -> entry.market.iconUrl
            is MarketListEntry.Perpetual -> entry.market.iconUrl
        }
    val marketValue =
        when (entry) {
            is MarketListEntry.Spot -> entry.market.marketCap
            is MarketListEntry.Perpetual -> entry.market.volume
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(1.dp, MixinAppTheme.colors.borderColor, shape)
                .clip(shape)
                .selectable(
                    selected = selected,
                    role = Role.Checkbox,
                    onClick = onSelect,
                )
                .padding(vertical = 10.dp)
                .padding(start = 12.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MarketIcon(url = iconUrl, size = 24.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = symbol,
                    color = MixinAppTheme.colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
                if (entry is MarketListEntry.Perpetual) {
                    Spacer(modifier = Modifier.width(4.dp))
                    MarketPerpBadge()
                }
            }
            Text(
                text = formatSpotVolume(marketValue),
                color = MixinAppTheme.colors.textAssist,
                fontSize = 13.sp,
                maxLines = 1,
            )
        }
        Icon(
            painter =
                painterResource(
                    if (selected) {
                        R.drawable.ic_asset_add_checked
                    } else {
                        R.drawable.ic_asset_add_unchecked
                    },
                ),
            contentDescription =
                stringResource(
                    if (selected) {
                        R.string.Remove_from_Watchlist
                    } else {
                        R.string.Add_to_Watchlist
                    },
                ),
            tint = Color.Unspecified,
            modifier = Modifier.size(30.dp),
        )
    }
}

@Composable
private fun MarketRow(
    entry: MarketListEntry,
    settings: MarketDisplaySettings,
    showMarketCap: Boolean,
    onFavorite: () -> Unit,
    onClick: () -> Unit,
) {
    var favoriteAnimationTrigger by remember(entry.stableId) { mutableIntStateOf(0) }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = 16.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier =
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable {
                        if (!entry.isFavored) {
                            favoriteAnimationTrigger++
                        }
                        onFavorite()
                    },
        ) {
            MarketFavoriteIcon(
                isFavored = entry.isFavored,
                unselectedIconRes = R.drawable.ic_asset_favorites,
                selectedIconRes = R.drawable.ic_asset_favorites_checked,
                contentDescription =
                    stringResource(
                        if (entry.isFavored) {
                            R.string.Remove_from_Watchlist
                        } else {
                            R.string.Add_to_Watchlist
                        },
                    ),
                modifier = Modifier.size(16.dp),
                animationTrigger = favoriteAnimationTrigger,
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        when (entry) {
            is MarketListEntry.Spot ->
                SpotMarketRowContent(
                    market = entry.market,
                    settings = settings,
                    showMarketCap = showMarketCap,
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
    showMarketCap: Boolean,
) {
    val change = market.changePercent(settings.priceChangePeriod)
    MarketIcon(url = market.iconUrl, size = 38.dp)
    Spacer(modifier = Modifier.width(10.dp))
    Column {
        Text(
            text = market.symbol,
            color = MixinAppTheme.colors.textPrimary,
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.height(2.dp))
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
                        .background(MixinAppTheme.colors.backgroundGrayLight, RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = formatSpotVolume(if (showMarketCap) market.marketCap else market.totalVolume),
                color = MixinAppTheme.colors.textAssist,
                fontSize = 12.sp,
                maxLines = 1,
            )
        }
    }
    BasicText(
        text = formatSpotPrice(market.currentPrice),
        style =
            TextStyle(
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End,
            ),
        maxLines = 1,
        softWrap = false,
        autoSize =
            TextAutoSize.StepBased(
                minFontSize = 8.sp,
                maxFontSize = 14.sp,
                stepSize = 0.5.sp,
            ),
        modifier = Modifier.weight(1f),
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
    MarketIcon(url = market.iconUrl, size = 38.dp)
    Spacer(modifier = Modifier.width(10.dp))
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = market.tokenSymbol,
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 14.sp,
            )
            Spacer(modifier = Modifier.width(5.dp))
            MarketPerpBadge()
        }
    }
    BasicText(
        text = "$${market.last}",
        style =
            TextStyle(
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End,
            ),
        maxLines = 1,
        softWrap = false,
        autoSize =
            TextAutoSize.StepBased(
                minFontSize = 8.sp,
                maxFontSize = 14.sp,
                stepSize = 0.5.sp,
            ),
        modifier = Modifier.weight(1f),
    )
    Spacer(modifier = Modifier.width(16.dp))
    ChangeColumn(
        change = change,
        sparkline = null,
        quoteColorReversed = settings.quoteColorReversed,
        fontSize = 14.sp,
    )
}

@Composable
private fun MarketPerpBadge() {
    Text(
        text = "Perp",
        fontSize = 12.sp,
        color = MixinAppTheme.colors.textAssist,
        lineHeight = 14.sp,
        modifier =
            Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(MixinAppTheme.colors.backgroundGrayLight)
                .padding(horizontal = 3.dp, vertical = 1.dp),
    )
}

@Composable
private fun MarketIcon(
    url: String,
    size: Dp = 34.dp,
) {
    CoilImage(
        model = url,
        placeholder = R.drawable.ic_avatar_place_holder,
        modifier =
            Modifier
                .size(size)
                .clip(CircleShape),
    )
}

@Composable
private fun ChangeColumn(
    change: BigDecimal?,
    sparkline: String?,
    quoteColorReversed: Boolean,
    fontSize: TextUnit = 12.sp,
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
            contentAlignment = Alignment.CenterEnd,
        ) {
            Text(
                text = change?.let(::formatPercent) ?: "--",
                color = changeColor,
                fontSize = fontSize,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End,
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
                        .width(60.dp)
                        .height(20.dp),
            )
            Text(
                text = change?.let(::formatPercent) ?: "--",
                color = changeColor,
                fontSize = fontSize,
                fontWeight = FontWeight.Medium,
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
                text = "${stringResource(R.string.Bitcoin)} ${stringResource(R.string.Dominance)}",
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
                                    priceChangePeriodLabel(MarketPriceChangePeriod.TWENTY_FOUR_HOURS) to MarketPriceChangePeriod.TWENTY_FOUR_HOURS,
                                    priceChangePeriodLabel(MarketPriceChangePeriod.SEVEN_DAYS) to MarketPriceChangePeriod.SEVEN_DAYS,
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
                    fontSize = 12.sp,
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
