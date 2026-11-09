package one.mixin.android.ui.wallet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.numberFormat8
import one.mixin.android.ui.home.web3.market.MarketChip
import one.mixin.android.ui.home.web3.market.MarketIcon
import one.mixin.android.ui.home.web3.market.MarketSearchBar
import one.mixin.android.ui.home.web3.market.SearchResultSection
import one.mixin.android.ui.home.web3.trade.perps.PerpsLeverageBadge
import one.mixin.android.ui.wallet.home.components.AccountApy

@Composable
internal fun WalletSearchPage(
    state: WalletSearchUiState,
    recentSearches: List<WalletSearchResult>,
    onQueryChanged: (String) -> Unit,
    onCancel: () -> Unit,
    onSelectTab: (WalletSearchTab) -> Unit,
    onClearRecent: () -> Unit,
    onResultClick: (WalletSearchResult) -> Unit,
    onSearchMarkets: () -> Unit,
) {
    val hasQuery = state.query.isNotBlank()
    val tabs = walletSearchTabs(state.results.map { it.tab })
    val quoteColorReversed = LocalContext.current.defaultSharedPreferences.getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    val listState = rememberLazyListState()
    LaunchedEffect(state.query, state.selectedTab) { listState.scrollToItem(0) }
    Column(Modifier.fillMaxSize().background(MixinAppTheme.colors.background)) {
        MarketSearchBar(
            state.query,
            onQueryChanged,
            onCancel,
            placeholder = stringResource(R.string.search_placeholder_asset),
            placeholderColor = MixinAppTheme.colors.textRemarks,
        )
        if (hasQuery) {
            val showSections = !state.isSearching && state.results.isNotEmpty() && state.selectedTab == WalletSearchTab.ALL
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = if (showSections) 0.dp else 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                tabs.forEach { tab ->
                    MarketChip(walletSearchTabLabel(tab), tab == state.selectedTab, { onSelectTab(tab) }, height = 38.dp)
                }
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f).background(if (hasQuery) MixinAppTheme.colors.backgroundWindow else MixinAppTheme.colors.background),
            state = listState,
        ) {
            if (!hasQuery) {
                if (recentSearches.isNotEmpty()) {
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 20.dp, top = 12.dp, end = 16.dp, bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(stringResource(R.string.Recent), color = MixinAppTheme.colors.textMinor, fontSize = 14.sp, lineHeight = 18.sp)
                            IconButton(onClick = onClearRecent, modifier = Modifier.size(32.dp)) {
                                Icon(painterResource(R.drawable.ic_market_search_delete), stringResource(R.string.Clear), tint = MixinAppTheme.colors.iconGray)
                            }
                        }
                    }
                    item {
                        Column {
                            WalletRecentSearchFlow(recentSearches, onResultClick)
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            } else {
                if (state.isSearching) {
                    item {
                        Box(Modifier.fillMaxWidth().background(MixinAppTheme.colors.background).padding(36.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(20.dp), color = MixinAppTheme.colors.accent, strokeWidth = 2.dp)
                        }
                    }
                } else if (state.results.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().height(292.dp).background(MixinAppTheme.colors.background).padding(bottom = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Image(painterResource(R.drawable.ic_empty_file), contentDescription = null)
                            Spacer(Modifier.height(12.dp))
                            Text(stringResource(R.string.NO_RESULTS), color = MixinAppTheme.colors.textRemarks, fontSize = 14.sp)
                        }
                    }
                } else if (state.selectedTab == WalletSearchTab.ALL) {
                    tabs.filter { it != WalletSearchTab.ALL }.forEachIndexed { index, tab ->
                        if (index > 0) item { Spacer(Modifier.height(6.dp)) }
                        item {
                            val results = state.results.filter { it.tab == tab }
                            SearchResultSection(
                                walletSearchTabLabel(tab),
                                walletSearchHasMore(results.size),
                                { onSelectTab(tab) },
                                titlePadding = PaddingValues(start = 20.dp, top = if (index == 0) 15.dp else 19.dp, end = 20.dp, bottom = 5.dp),
                            ) {
                                walletSearchPreview(results, all = true).forEach { result ->
                                    WalletSearchRow(result, quoteColorReversed) { onResultClick(result) }
                                }
                                Spacer(Modifier.height(10.dp))
                            }
                        }
                    }
                } else {
                    items(state.results.filter { it.tab == state.selectedTab }) { result ->
                        WalletSearchRow(result, quoteColorReversed) { onResultClick(result) }
                    }
                }
                if (!state.isSearching) {
                    item {
                        Spacer(Modifier.fillMaxWidth().height(6.dp).background(MixinAppTheme.colors.backgroundWindow))
                        Row(
                            Modifier.fillMaxWidth().background(MixinAppTheme.colors.background).clickable(onClick = onSearchMarkets)
                                .heightIn(min = 64.dp).padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(R.string.search_query_in_markets, state.query.trim()),
                                color = MixinAppTheme.colors.accent,
                                fontSize = 16.sp,
                                lineHeight = 20.sp,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(12.dp))
                            Icon(painterResource(R.drawable.ic_arrow_right), contentDescription = null, tint = MixinAppTheme.colors.textRemarks, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun walletSearchTabLabel(tab: WalletSearchTab): String = stringResource(
    when (tab) {
        WalletSearchTab.ALL -> R.string.All
        WalletSearchTab.ASSETS -> R.string.Assets
        WalletSearchTab.POSITIONS -> R.string.perps_positions
        WalletSearchTab.EARN -> R.string.Earn
        WalletSearchTab.COLLECTIBLES -> R.string.Collectibles
    },
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WalletRecentSearchFlow(
    results: List<WalletSearchResult>,
    onResultClick: (WalletSearchResult) -> Unit,
) {
    SubcomposeLayout(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) { constraints ->
        val contentWidth = constraints.maxWidth
        val loose = constraints.copy(minWidth = 0, maxWidth = contentWidth)
        val preferred = subcompose("preferred") {
            results.forEach { result ->
                Box(Modifier.width(IntrinsicSize.Max)) {
                    WalletRecentSearchChip(result) { onResultClick(result) }
                }
            }
        }.map { it.measure(loose).width }
        val rows = walletRecentChipRows(preferred, contentWidth, 16.dp.roundToPx())
        val visible = rows.flatten()
        val maxWidths = IntArray(results.size) { contentWidth }
        visible.forEach { maxWidths[it.index] = it.width }
        val maxWidthDp = maxWidths.map { it.toDp() }
        val flow = subcompose("flow") {
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxLines = 3,
            ) {
                visible.forEach { placement ->
                    WalletRecentSearchChip(results[placement.index], maxWidthDp[placement.index]) {
                        onResultClick(results[placement.index])
                    }
                }
            }
        }.single().measure(constraints)
        layout(flow.width, flow.height) { flow.placeRelative(0, 0) }
    }
}

@Composable
private fun WalletRecentSearchChip(
    result: WalletSearchResult,
    maxWidth: Dp? = null,
    onClick: () -> Unit,
) {
    val isCollectible = result is WalletSearchResult.Collectible
    Row(
        Modifier
            .then(if (maxWidth != null) Modifier.widthIn(max = maxWidth) else Modifier)
            .border(
                BorderStroke(1.dp, MixinAppTheme.colors.textPrimary.copy(alpha = 0.06f)),
                CircleShape,
            )
            .clip(CircleShape)
            .background(MixinAppTheme.colors.background)
            .clickable(onClick = onClick)
            .padding(start = if (isCollectible) 7.dp else 6.dp, top = 4.dp, bottom = 4.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (result is WalletSearchResult.Collectible) {
            Box(Modifier.width(30.dp).height(32.dp), contentAlignment = Alignment.Center) {
                CoilImage(
                    model = result.collectible.iconURL?.takeIf { it.isNotBlank() } ?: result.collectible.contentURL,
                    placeholder = R.drawable.ic_avatar_place_holder,
                    modifier = Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
        } else {
            WalletSearchIcon(result, 32.dp)
        }
        Spacer(Modifier.width(if (isCollectible) 5.dp else 4.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when (result) {
                        is WalletSearchResult.Asset -> result.token.name
                        is WalletSearchResult.Position -> result.tokenName
                        is WalletSearchResult.Collectible -> result.collectible.name
                        is WalletSearchResult.Earn -> result.account.assetSymbol
                    },
                    color = MixinAppTheme.colors.textPrimary,
                    fontSize = 14.sp,
                    lineHeight = 16.sp,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (result is WalletSearchResult.Position) {
                    Spacer(Modifier.width(3.dp))
                    val quoteColorReversed = LocalContext.current.defaultSharedPreferences.getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
                    val color = if (result.position.side.equals("long", true) != quoteColorReversed) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
                    PerpsLeverageBadge(result.position.leverage, color = color)
                } else if (result is WalletSearchResult.Earn) {
                    result.account.maxApyText?.let {
                        val quoteColorReversed = LocalContext.current.defaultSharedPreferences.getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
                        Spacer(Modifier.width(4.dp))
                        AccountApy(stringResource(R.string.cash_account_apy, it), quoteColorReversed)
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                walletSearchSubtitle(result, recent = true),
                color = MixinAppTheme.colors.textAssist,
                fontSize = 13.sp,
                lineHeight = 15.sp,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun WalletSearchRow(result: WalletSearchResult, quoteColorReversed: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(MixinAppTheme.colors.background).clickable(onClick = onClick)
            .heightIn(min = 70.dp).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WalletSearchIcon(result, 42.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when (result) {
                        is WalletSearchResult.Asset -> "${result.token.balance.numberFormat8()} ${result.token.symbol}"
                        is WalletSearchResult.Position -> "${stringResource(if (result.position.side.equals("long", true)) R.string.Long else R.string.Short)} ${result.position.tokenSymbol.orEmpty()}"
                        is WalletSearchResult.Earn -> result.account.assetSymbol
                        is WalletSearchResult.Collectible -> result.collectible.name
                    },
                    color = MixinAppTheme.colors.textPrimary,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                when (result) {
                    is WalletSearchResult.Position -> {
                        val color = if (result.position.side.equals("long", true) != quoteColorReversed) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
                        Spacer(Modifier.width(4.dp))
                        PerpsLeverageBadge(result.position.leverage, color = color)
                    }
                    is WalletSearchResult.Earn -> result.account.maxApyText?.let {
                        Spacer(Modifier.width(4.dp))
                        AccountApy(stringResource(R.string.cash_account_apy, it), quoteColorReversed)
                    }
                    else -> Unit
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(walletSearchSubtitle(result), color = MixinAppTheme.colors.textAssist, fontSize = 14.sp, lineHeight = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun walletSearchSubtitle(result: WalletSearchResult, recent: Boolean = false): String = when (result) {
    is WalletSearchResult.Asset -> if (recent) "${result.token.balance.numberFormat8()} ${result.token.symbol}" else result.token.name
    is WalletSearchResult.Position -> "${result.position.quantity.toBigDecimalOrNull()?.abs()?.numberFormat8().orEmpty()} ${result.position.tokenSymbol.orEmpty()}"
    is WalletSearchResult.Earn -> result.amount
    is WalletSearchResult.Collectible -> "#${result.collectible.sequence}"
}

@Composable
private fun WalletSearchIcon(result: WalletSearchResult, size: Dp) {
    when (result) {
        is WalletSearchResult.Asset -> Box(modifier = Modifier.size(size)) {
            CoilImage(
                model = result.token.iconUrl,
                placeholder = R.drawable.ic_avatar_place_holder,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            if (result.token.collectionHash.isNullOrEmpty() && !result.token.chainIconUrl.isNullOrBlank()) {
                CoilImage(
                    model = result.token.chainIconUrl,
                    placeholder = R.drawable.ic_avatar_place_holder,
                    modifier = Modifier
                        .size(13.dp)
                        .align(Alignment.BottomStart)
                        .offset(x = (-1).dp, y = 1.dp)
                        .clip(CircleShape)
                        .background(MixinAppTheme.colors.background)
                        .border(1.dp, MixinAppTheme.colors.background, CircleShape),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        is WalletSearchResult.Position -> MarketIcon(result.position.iconUrl.orEmpty(), size)
        is WalletSearchResult.Earn -> MarketIcon(result.account.iconUrl, size)
        is WalletSearchResult.Collectible -> CoilImage(
            model = result.collectible.iconURL?.takeIf { it.isNotBlank() } ?: result.collectible.contentURL,
            placeholder = R.drawable.ic_avatar_place_holder,
            modifier = Modifier.size(size).clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
        )
    }
}
