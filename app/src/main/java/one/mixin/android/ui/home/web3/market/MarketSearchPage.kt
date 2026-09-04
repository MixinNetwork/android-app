package one.mixin.android.ui.home.web3.market

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormatCompact
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.priceFormat
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.RecentSearch
import one.mixin.android.vo.RecentSearchType
import one.mixin.android.vo.market.MarketItem
import java.math.BigDecimal

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MarketSearchPage(
    state: MarketSearchUiState,
    recentSearches: List<MarketRecentSearch>,
    onQueryChanged: (String) -> Unit,
    onCancel: () -> Unit,
    onSelectTab: (MarketSearchTab) -> Unit,
    onClearRecentSearches: () -> Unit,
    onRecentSearchClick: (RecentSearch) -> Unit,
    onSpotMarketClick: (MarketItem) -> Unit,
    onPerpetualMarketClick: (PerpsMarket) -> Unit,
) {
    val marketRecentSearches = recentSearches
    val tabs = marketSearchTabs(state.query)
    val quoteColorReversed =
        androidx.compose.ui.platform.LocalContext.current.defaultSharedPreferences
            .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    val listState = rememberLazyListState()
    LaunchedEffect(state.hasQuery) {
        if (!state.hasQuery) {
            listState.scrollToItem(0)
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MixinAppTheme.colors.background),
    ) {
        MarketSearchBar(
            query = state.query,
            onQueryChanged = onQueryChanged,
            onCancel = onCancel,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
        ) {
            if (state.query.isBlank() && marketRecentSearches.isNotEmpty()) {
                item(key = "recent_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.Recent),
                            color = MixinAppTheme.colors.textAssist,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 16.dp),
                        )
                        IconButton(
                            onClick = onClearRecentSearches,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_market_search_delete),
                                contentDescription = stringResource(R.string.Clear),
                                tint = Color.Unspecified,
                            )
                        }
                    }
                }
                item(key = "recent_items") {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        marketRecentSearches.forEach { search ->
                            MarketRecentSearchChip(
                                search = search,
                                quoteColorReversed = quoteColorReversed,
                                onClick = { onRecentSearchClick(search.search) },
                            )
                        }
                    }
                }
                item(key = "recent_divider") {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(MixinAppTheme.colors.borderPrimary),
                    )
                }
            }

            item(key = "tabs") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    tabs.forEach { tab ->
                        MarketChip(
                            text = marketSearchTabLabel(tab),
                            selected = tab == state.selectedTab,
                            onClick = { onSelectTab(tab) },
                        )
                    }
                }
            }

            if (state.isSearching) {
                item(key = "searching") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = MixinAppTheme.colors.accent,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            } else if (!state.hasQuery) {
                when (state.selectedTab) {
                    MarketSearchTab.CRYPTO -> {
                        items(
                            items = state.spotTrending,
                            key = { "trending_spot:${it.coinId}" },
                        ) { market ->
                            SpotMarketSearchRow(
                                market = market,
                                quoteColorReversed = quoteColorReversed,
                                onClick = { onSpotMarketClick(market) },
                            )
                        }
                    }
                    MarketSearchTab.PERPETUAL -> {
                        items(
                            items = state.perpetualTrending,
                            key = { "trending_perpetual:${it.marketId}" },
                        ) { market ->
                            PerpetualMarketSearchRow(
                                market = market,
                                quoteColorReversed = quoteColorReversed,
                                onClick = { onPerpetualMarketClick(market) },
                            )
                        }
                    }
                    MarketSearchTab.ALL -> Unit
                }
            } else {
                when (state.selectedTab) {
                    MarketSearchTab.ALL -> {
                        item(key = "all_results") {
                            Column {
                                if (state.spotResults.isNotEmpty()) {
                                    SearchResultSection(
                                        title = stringResource(R.string.Crypto),
                                        hasMore = state.spotResults.size > SEARCH_RESULT_PREVIEW_COUNT,
                                        onMore = { onSelectTab(MarketSearchTab.CRYPTO) },
                                    ) {
                                        state.spotResults
                                            .take(SEARCH_RESULT_PREVIEW_COUNT)
                                            .forEach { market ->
                                                SpotMarketSearchRow(
                                                    market = market,
                                                    quoteColorReversed = quoteColorReversed,
                                                    onClick = { onSpotMarketClick(market) },
                                                )
                                        }
                                    }
                                }
                                if (state.spotResults.isNotEmpty() && state.perpetualResults.isNotEmpty()) {
                                    Spacer(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .background(MixinAppTheme.colors.backgroundWindow),
                                    )
                                }
                                if (state.perpetualResults.isNotEmpty()) {
                                    SearchResultSection(
                                        title = stringResource(R.string.Perpetual),
                                        hasMore = state.perpetualResults.size > SEARCH_RESULT_PREVIEW_COUNT,
                                        onMore = { onSelectTab(MarketSearchTab.PERPETUAL) },
                                    ) {
                                        state.perpetualResults
                                            .take(SEARCH_RESULT_PREVIEW_COUNT)
                                            .forEach { market ->
                                                PerpetualMarketSearchRow(
                                                    market = market,
                                                    quoteColorReversed = quoteColorReversed,
                                                    onClick = { onPerpetualMarketClick(market) },
                                                )
                                            }
                                    }
                                }
                                if (state.spotResults.isEmpty() && state.perpetualResults.isEmpty()) {
                                    SearchEmptyResult()
                                }
                            }
                        }
                    }
                    MarketSearchTab.CRYPTO -> {
                        if (state.spotResults.isEmpty()) {
                            item(key = "empty_spot_results") { SearchEmptyResult() }
                        } else {
                            items(
                                items = state.spotResults,
                                key = { "search_spot:${it.coinId}" },
                            ) { market ->
                                SpotMarketSearchRow(
                                    market = market,
                                    quoteColorReversed = quoteColorReversed,
                                    onClick = { onSpotMarketClick(market) },
                                )
                            }
                        }
                    }
                    MarketSearchTab.PERPETUAL -> {
                        if (state.perpetualResults.isEmpty()) {
                            item(key = "empty_perpetual_results") { SearchEmptyResult() }
                        } else {
                            items(
                                items = state.perpetualResults,
                                key = { "search_perpetual:${it.marketId}" },
                            ) { market ->
                                PerpetualMarketSearchRow(
                                    market = market,
                                    quoteColorReversed = quoteColorReversed,
                                    onClick = { onPerpetualMarketClick(market) },
                                )
                            }
                        }
                    }
                }
            }
            item(key = "bottom_spacing") { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun MarketSearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    onCancel: () -> Unit,
) {
    val focusRequester = androidx.compose.runtime.remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .clip(CircleShape)
                .background(MixinAppTheme.colors.backgroundWindow)
                .focusRequester(focusRequester)
                .padding(horizontal = 12.dp),
            textStyle = TextStyle(
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
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.Search),
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
            modifier = Modifier
                .clip(CircleShape)
                .clickable {
                    keyboardController?.hide()
                    onCancel()
                }
                .padding(vertical = 8.dp),
        )
    }
}

@Composable
private fun MarketRecentSearchChip(
    search: MarketRecentSearch,
    quoteColorReversed: Boolean,
    onClick: () -> Unit,
) {
    val recentSearch = search.search
    Row(
        modifier = Modifier
            .wrapContentWidth()
            .border(
                BorderStroke(1.dp, MixinAppTheme.colors.textPrimary.copy(alpha = 0.06f)),
                RoundedCornerShape(24.dp),
            )
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(start = 6.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MarketIcon(url = recentSearch.iconUrl.orEmpty(), size = 32.dp)
        Spacer(modifier = Modifier.width(6.dp))
        Column(
            modifier = Modifier
                .wrapContentWidth()
                .height(32.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = recentSearch.title.orEmpty(),
                    color = MixinAppTheme.colors.textPrimary,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp,
                    modifier = Modifier.widthIn(min = 34.dp),
                )
                if (recentSearch.type == RecentSearchType.PERPETUAL) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = stringResource(R.string.Perp),
                        color = MixinAppTheme.colors.textAssist,
                        fontSize = 11.sp,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                        modifier = Modifier
                            .background(
                                MixinAppTheme.colors.marketBadgeBackground,
                                RoundedCornerShape(4.dp),
                            )
                            .padding(horizontal = 2.dp),
                    )
                }
            }
            Text(
                text = formatSearchPercent(search.change),
                color = searchChangeColor(search.change, quoteColorReversed),
                fontSize = 13.sp,
                lineHeight = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                modifier = Modifier.widthIn(min = 34.dp),
            )
        }
    }
}

@Composable
private fun SearchResultSection(
    title: String,
    hasMore: Boolean,
    onMore: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 14.sp,
            )
            if (hasMore) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.More),
                    color = MixinAppTheme.colors.accent,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onMore)
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
        }
        content()
    }
}

@Composable
private fun SpotMarketSearchRow(
    market: MarketItem,
    quoteColorReversed: Boolean,
    onClick: () -> Unit,
) {
    MarketSearchRow(
        iconUrl = market.iconUrl,
        title = market.symbol,
        volume = formatSearchSpotVolume(market.totalVolume),
        price = formatSearchSpotPrice(market.currentPrice),
        change = market.priceChangePercentage24H.toBigDecimalOrNull(),
        quoteColorReversed = quoteColorReversed,
        onClick = onClick,
    )
}

@Composable
private fun PerpetualMarketSearchRow(
    market: PerpsMarket,
    quoteColorReversed: Boolean,
    onClick: () -> Unit,
) {
    MarketSearchRow(
        iconUrl = market.iconUrl,
        title = market.displaySymbol.ifBlank { market.tokenSymbol },
        badge = stringResource(R.string.Perp),
        volume = formatSearchPerpetualVolume(market.volume),
        price = formatPerpsMarketListPrice(market.last),
        change = market.changePercentValue(),
        quoteColorReversed = quoteColorReversed,
        onClick = onClick,
    )
}

@Composable
private fun MarketSearchRow(
    iconUrl: String,
    title: String,
    volume: String,
    price: String,
    change: BigDecimal?,
    quoteColorReversed: Boolean,
    onClick: () -> Unit,
    badge: String? = null,
) {
    val changeColor = searchChangeColor(change, quoteColorReversed)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MarketIcon(url = iconUrl, size = 38.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = MixinAppTheme.colors.textPrimary,
                    fontSize = 14.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                )
                if (badge != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = badge,
                        color = MixinAppTheme.colors.textAssist,
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                        modifier = Modifier
                            .background(
                                MixinAppTheme.colors.marketBadgeBackground,
                                RoundedCornerShape(4.dp),
                            )
                            .padding(horizontal = 2.dp),
                    )
                }
            }
            Text(
                text = stringResource(R.string.volume_label, volume),
                color = MixinAppTheme.colors.textAssist,
                fontSize = 12.sp,
                lineHeight = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
            )
        }
        Column(
            modifier = Modifier.width(96.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = price,
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
            )
            Text(
                text = formatSearchPercent(change),
                color = changeColor,
                fontSize = 14.sp,
                maxLines = 1,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun SearchEmptyResult() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.NO_RESULTS),
            color = MixinAppTheme.colors.textAssist,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun marketSearchTabLabel(tab: MarketSearchTab): String =
    when (tab) {
        MarketSearchTab.ALL -> stringResource(R.string.All)
        MarketSearchTab.CRYPTO -> stringResource(R.string.Crypto)
        MarketSearchTab.PERPETUAL -> stringResource(R.string.Perpetual)
    }

private fun formatSearchSpotPrice(value: String): String =
    runCatching {
        "${Fiats.getSymbol()}${BigDecimal(value).multiply(BigDecimal(Fiats.getRate())).priceFormat()}"
    }.getOrDefault(value)

private fun formatSearchSpotVolume(value: String): String =
    runCatching {
        "${Fiats.getSymbol()}${BigDecimal(value).multiply(BigDecimal(Fiats.getRate())).numberFormatCompact()}"
    }.getOrDefault(value)

private fun formatSearchPerpetualVolume(value: String): String =
    runCatching { BigDecimal(value).numberFormatCompact() }.getOrDefault(value)

private fun formatSearchPercent(change: BigDecimal?): String {
    if (change == null) return "--"
    val prefix = if (change > BigDecimal.ZERO) "+" else ""
    return "$prefix${change.numberFormat2()}%"
}

@Composable
private fun searchChangeColor(
    change: BigDecimal?,
    quoteColorReversed: Boolean,
): Color =
    when {
        change == null -> MixinAppTheme.colors.textAssist
        change >= BigDecimal.ZERO ->
            if (quoteColorReversed) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
        else ->
            if (quoteColorReversed) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
    }

private const val SEARCH_RESULT_PREVIEW_COUNT = 3
