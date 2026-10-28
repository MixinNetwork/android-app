package one.mixin.android.ui.wallet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import one.mixin.android.ui.home.web3.market.SearchEmptyResult
import one.mixin.android.ui.home.web3.market.SearchResultSection
import one.mixin.android.ui.home.web3.trade.perps.PerpsLeverageBadge
import one.mixin.android.ui.wallet.home.WalletAssetIcon
import one.mixin.android.ui.wallet.home.components.AccountApy

@OptIn(ExperimentalLayoutApi::class)
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
        MarketSearchBar(state.query, onQueryChanged, onCancel, placeholder = stringResource(R.string.search_placeholder_asset))
        if (hasQuery) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                tabs.forEach { tab ->
                    MarketChip(walletSearchTabLabel(tab), tab == state.selectedTab, { onSelectTab(tab) })
                }
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(if (hasQuery) MixinAppTheme.colors.backgroundWindow else MixinAppTheme.colors.background),
            state = listState,
        ) {
            if (!hasQuery) {
                if (recentSearches.isNotEmpty()) {
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(stringResource(R.string.Recent), color = MixinAppTheme.colors.textAssist, fontSize = 14.sp, modifier = Modifier.padding(vertical = 16.dp))
                            IconButton(onClick = onClearRecent, modifier = Modifier.size(32.dp)) {
                                Icon(painterResource(R.drawable.ic_market_search_delete), stringResource(R.string.Clear), tint = Color.Unspecified)
                            }
                        }
                    }
                    item {
                        Column {
                            FlowRow(
                                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                maxLines = 3,
                            ) {
                                recentSearches.forEach { result -> WalletRecentSearchChip(result) { onResultClick(result) } }
                            }
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
                    item { Box(Modifier.fillMaxWidth().background(MixinAppTheme.colors.background)) { SearchEmptyResult() } }
                } else if (state.selectedTab == WalletSearchTab.ALL) {
                    tabs.filter { it != WalletSearchTab.ALL }.forEachIndexed { index, tab ->
                        if (index > 0) item { Spacer(Modifier.height(8.dp)) }
                        item {
                            val results = state.results.filter { it.tab == tab }
                            SearchResultSection(walletSearchTabLabel(tab), walletSearchHasMore(results.size), { onSelectTab(tab) }) {
                                walletSearchPreview(results, all = true).forEach { result ->
                                    WalletSearchRow(result, quoteColorReversed) { onResultClick(result) }
                                }
                            }
                        }
                    }
                } else {
                    items(state.results.filter { it.tab == state.selectedTab }) { result ->
                        WalletSearchRow(result, quoteColorReversed) { onResultClick(result) }
                    }
                }
                item {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth().background(MixinAppTheme.colors.background).clickable(onClick = onSearchMarkets)
                            .heightIn(min = 56.dp).padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.search_query_in_markets, state.query.trim()),
                            color = MixinAppTheme.colors.accent,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(12.dp))
                        Icon(painterResource(R.drawable.ic_arrow_right), contentDescription = null, tint = MixinAppTheme.colors.textAssist, modifier = Modifier.size(16.dp))
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

@Composable
private fun WalletRecentSearchChip(result: WalletSearchResult, onClick: () -> Unit) {
    val maxChipWidth = ((LocalConfiguration.current.screenWidthDp - 40 - 16) / 2).dp
    Row(
        Modifier
            .widthIn(max = maxChipWidth)
            .border(
                BorderStroke(1.dp, MixinAppTheme.colors.textPrimary.copy(alpha = 0.06f)),
                RoundedCornerShape(32.dp),
            )
            .clip(RoundedCornerShape(21.dp))
            .background(MixinAppTheme.colors.background)
            .clickable(onClick = onClick)
            .padding(start = 6.dp, top = 5.dp, bottom = 5.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WalletSearchIcon(result, 32.dp)
        Spacer(Modifier.width(4.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when (result) {
                        is WalletSearchResult.Asset -> result.token.name
                        is WalletSearchResult.Position -> result.tokenName
                        else -> ""
                    },
                    color = MixinAppTheme.colors.textPrimary,
                    fontSize = 14.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (result is WalletSearchResult.Position) {
                    Spacer(Modifier.width(3.dp))
                    PerpsLeverageBadge(result.position.leverage)
                }
            }
            Text(
                walletSearchSubtitle(result, recent = true),
                color = MixinAppTheme.colors.textAssist,
                fontSize = 12.sp,
                lineHeight = 12.sp,
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
            .heightIn(min = 64.dp).padding(horizontal = 20.dp, vertical = 10.dp),
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                when (result) {
                    is WalletSearchResult.Position -> {
                        Spacer(Modifier.width(4.dp))
                        PerpsLeverageBadge(result.position.leverage)
                    }
                    is WalletSearchResult.Earn -> result.account.maxApyText?.let {
                        Spacer(Modifier.width(4.dp))
                        AccountApy(stringResource(R.string.cash_account_apy, it), quoteColorReversed)
                    }
                    else -> Unit
                }
            }
            Text(walletSearchSubtitle(result), color = MixinAppTheme.colors.textAssist, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        is WalletSearchResult.Asset -> WalletAssetIcon(result.token.iconUrl, result.token.chainIconUrl, result.token.collectionHash, size)
        is WalletSearchResult.Position -> MarketIcon(result.position.iconUrl.orEmpty(), size)
        is WalletSearchResult.Earn -> MarketIcon(result.account.iconUrl, size)
        is WalletSearchResult.Collectible -> CoilImage(
            model = result.collectible.iconURL?.takeIf { it.isNotBlank() } ?: result.collectible.contentURL,
            placeholder = R.drawable.ic_avatar_place_holder,
            modifier = Modifier.size(size).clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
        )
    }
}
