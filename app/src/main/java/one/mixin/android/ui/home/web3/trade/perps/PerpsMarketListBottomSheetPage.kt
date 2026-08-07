package one.mixin.android.ui.home.web3.trade.perps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.home.web3.market.MarketChip
import one.mixin.android.ui.home.web3.market.MarketHeaderSortLabels
import one.mixin.android.ui.home.web3.market.MarketListEntry
import one.mixin.android.ui.home.web3.market.MarketListFavoriteIconSize
import one.mixin.android.ui.home.web3.market.MarketPriceChangePeriod
import one.mixin.android.ui.home.web3.market.MarketRecommendationCard
import one.mixin.android.ui.home.web3.market.MarketSortColumn
import one.mixin.android.ui.home.web3.market.PerpetualMarketBadgeStyle
import one.mixin.android.ui.home.web3.market.PerpsMarketListItem
import one.mixin.android.widget.components.MixinButton

@Composable
internal fun PerpsMarketListBottomSheetPage(
    state: PerpsMarketListUiState,
    onQueryChanged: (String) -> Unit,
    onCancel: () -> Unit,
    onCategorySelected: (PerpsMarketCategory) -> Unit,
    onSort: (MarketSortColumn) -> Unit,
    onFavorite: (PerpsMarket, Boolean, (Boolean) -> Unit) -> Unit,
    onMarketClick: (PerpsMarket) -> Unit,
    onRecommendationSelected: (PerpsMarket) -> Unit,
    onAddRecommendations: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MixinAppTheme.colors.background),
    ) {
        PerpsMarketSearchBar(
            query = state.query,
            onQueryChanged = onQueryChanged,
            onCancel = onCancel,
        )
        PerpsMarketCategories(
            selectedCategory = state.selectedCategory,
            onCategorySelected = onCategorySelected,
        )
        if (!state.isShowingRecommendations) {
            MarketHeaderSortLabels(
                period = MarketPriceChangePeriod.TWENTY_FOUR_HOURS,
                showMarketCap = false,
                sortState = state.sortState,
                onSort = onSort,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        PerpsMarketListContent(
            state = state,
            onFavorite = onFavorite,
            onMarketClick = onMarketClick,
            onRecommendationSelected = onRecommendationSelected,
            onAddRecommendations = onAddRecommendations,
        )
    }
}

@Composable
private fun PerpsMarketSearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    onCancel: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(start = 20.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier =
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(MixinAppTheme.colors.backgroundWindow)
                    .padding(horizontal = 12.dp),
            textStyle =
                TextStyle(
                    color = MixinAppTheme.colors.textPrimary,
                    fontSize = 14.sp,
                ),
            singleLine = true,
            cursorBrush = SolidColor(MixinAppTheme.colors.accent),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit_search),
                        contentDescription = null,
                        tint = MixinAppTheme.colors.iconGray,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_placeholder_market),
                                color = MixinAppTheme.colors.textAssist,
                                fontSize = 14.sp,
                            )
                        }
                        innerTextField()
                    }
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { onQueryChanged("") },
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_asset_add_search_clear),
                                contentDescription = stringResource(R.string.Clear),
                                tint = Color.Unspecified,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            },
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = stringResource(R.string.Cancel),
            color = MixinAppTheme.colors.accent,
            fontSize = 14.sp,
            modifier =
                Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onCancel)
                    .padding(vertical = 8.dp),
        )
    }
}

@Composable
private fun PerpsMarketCategories(
    selectedCategory: PerpsMarketCategory,
    onCategorySelected: (PerpsMarketCategory) -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(selectedCategory) {
        val selectedIndex = PerpsMarketCategory.entries.indexOf(selectedCategory)
        if (listState.layoutInfo.visibleItemsInfo.none { it.index == selectedIndex }) {
            listState.scrollToItem(selectedIndex)
            withFrameNanos { }
        }
        val selectedItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == selectedIndex }
        if (selectedItem != null) {
            val viewportCenter =
                (listState.layoutInfo.viewportStartOffset + listState.layoutInfo.viewportEndOffset) / 2
            val itemCenter = selectedItem.offset + selectedItem.size / 2
            listState.animateScrollBy((itemCenter - viewportCenter).toFloat())
        }
    }
    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(
            items = PerpsMarketCategory.entries,
            key = PerpsMarketCategory::name,
        ) { category ->
            if (category == PerpsMarketCategory.WATCHLIST) {
                WatchlistCategoryChip(
                    selected = selectedCategory == category,
                    onClick = { onCategorySelected(category) },
                )
            } else {
                MarketChip(
                    text = categoryLabel(category),
                    selected = selectedCategory == category,
                    onClick = { onCategorySelected(category) },
                )
            }
        }
    }
}

@Composable
private fun WatchlistCategoryChip(
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
        shape = CircleShape,
        border =
            BorderStroke(
                1.dp,
                if (selected) MixinAppTheme.colors.accent else MixinAppTheme.colors.borderPrimary,
            ),
        modifier =
            Modifier
                .size(width = 44.dp, height = 36.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_title_favorites),
                contentDescription = stringResource(R.string.Watchlist),
                tint = if (selected) MixinAppTheme.colors.accent else MixinAppTheme.colors.textPrimary,
                modifier = Modifier.size(MarketListFavoriteIconSize),
            )
        }
    }
}

@Composable
private fun PerpsMarketListContent(
    state: PerpsMarketListUiState,
    onFavorite: (PerpsMarket, Boolean, (Boolean) -> Unit) -> Unit,
    onMarketClick: (PerpsMarket) -> Unit,
    onRecommendationSelected: (PerpsMarket) -> Unit,
    onAddRecommendations: () -> Unit,
) {
    when {
        state.isShowingRecommendations -> {
            PerpsMarketRecommendations(
                state = state,
                onRecommendationSelected = onRecommendationSelected,
                onAddRecommendations = onAddRecommendations,
            )
        }

        state.visibleMarkets.isEmpty() -> {
            PerpsMarketEmptyState()
        }

        else -> {
            val listState = rememberLazyListState()
            LaunchedEffect(state.scrollToTopRequest) {
                listState.scrollToItem(0)
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                items(
                    items = state.visibleMarkets,
                    key = PerpsMarket::marketId,
                ) { market ->
                    val isFavored = state.isFavorite(market.marketId)
                    PerpsMarketListItem(
                        market = market,
                        isFavored = isFavored,
                        quoteColorReversed = state.quoteColorReversed,
                        badgeStyle = PerpetualMarketBadgeStyle.LEVERAGE,
                        onFavorite = { onResult -> onFavorite(market, isFavored, onResult) },
                        favoriteEnabled = !state.isFavoriteUpdatePending(market.marketId),
                        onClick = { onMarketClick(market) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PerpsMarketEmptyState() {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search_empty),
            contentDescription = null,
            tint = Color.Unspecified,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.NO_RESULTS),
            color = MixinAppTheme.colors.textAssist,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun PerpsMarketRecommendations(
    state: PerpsMarketListUiState,
    onRecommendationSelected: (PerpsMarket) -> Unit,
    onAddRecommendations: () -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        gridItems(
            items = state.recommendations,
            key = PerpsMarket::marketId,
        ) { market ->
            MarketRecommendationCard(
                entry =
                    MarketListEntry.Perpetual(
                        market = market,
                        isFavored = false,
                    ),
                selected = market.marketId in state.selectedRecommendationIds,
                quoteColorReversed = state.quoteColorReversed,
                perpetualBadgeStyle = PerpetualMarketBadgeStyle.LEVERAGE,
                onSelect = { onRecommendationSelected(market) },
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 15.dp),
                contentAlignment = Alignment.Center,
            ) {
                MixinButton(
                    onClick = onAddRecommendations,
                    enabled = state.selectedRecommendationIds.isNotEmpty() && !state.isAddingRecommendations,
                    backgroundColor = MixinAppTheme.colors.accent,
                    contentColor = Color.White,
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
                    )
                }
            }
        }
    }
}

@Composable
private fun categoryLabel(category: PerpsMarketCategory): String =
    stringResource(
        when (category) {
            PerpsMarketCategory.ALL -> R.string.perps_category_all
            PerpsMarketCategory.WATCHLIST -> R.string.Watchlist
            PerpsMarketCategory.CRYPTO -> R.string.perps_category_crypto
            PerpsMarketCategory.STOCKS -> R.string.perps_category_stocks
            PerpsMarketCategory.MEME -> R.string.perps_category_meme
            PerpsMarketCategory.INDICES -> R.string.perps_category_indices
            PerpsMarketCategory.COMMODITIES -> R.string.perps_category_commodities
            PerpsMarketCategory.FOREX -> R.string.perps_category_forex
        },
    )
