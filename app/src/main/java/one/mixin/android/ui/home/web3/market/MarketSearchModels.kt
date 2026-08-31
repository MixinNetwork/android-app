package one.mixin.android.ui.home.web3.market

import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.vo.RecentSearch
import one.mixin.android.vo.RecentSearchType
import one.mixin.android.vo.market.MarketItem
import java.math.BigDecimal
import java.util.Locale

internal enum class MarketSearchTab {
    ALL,
    CRYPTO,
    PERPETUAL,
}

internal fun marketSearchTabs(query: String): List<MarketSearchTab> =
    if (query.isBlank()) {
        listOf(MarketSearchTab.CRYPTO, MarketSearchTab.PERPETUAL)
    } else {
        MarketSearchTab.entries
    }

internal fun selectedMarketSearchTab(
    previousQuery: String,
    query: String,
    selectedTab: MarketSearchTab,
): MarketSearchTab =
    when {
        query.isBlank() -> MarketSearchTab.CRYPTO
        previousQuery.isBlank() -> MarketSearchTab.ALL
        else -> selectedTab
    }

internal fun List<PerpsMarket>.sortedForTrendingSearch(): List<PerpsMarket> =
    sortedWith(
        compareByDescending<PerpsMarket> { it.tradeVolumeScore1D }
            .thenByDescending { it.volume.toBigDecimalOrNull() ?: BigDecimal.ZERO }
            .thenBy { it.tokenSymbol.lowercase(Locale.ROOT) }
            .thenBy { it.marketId },
    )

internal data class MarketSearchUiState(
    val query: String = "",
    val selectedTab: MarketSearchTab = MarketSearchTab.CRYPTO,
    val spotTrending: List<MarketItem> = emptyList(),
    val perpetualTrending: List<PerpsMarket> = emptyList(),
    val spotResults: List<MarketItem> = emptyList(),
    val perpetualResults: List<PerpsMarket> = emptyList(),
    val isSearching: Boolean = false,
) {
    val hasQuery: Boolean
        get() = query.isNotBlank()

    val visibleSpotMarkets: List<MarketItem>
        get() = if (hasQuery) spotResults else spotTrending

    val visiblePerpetualMarkets: List<PerpsMarket>
        get() = if (hasQuery) perpetualResults else perpetualTrending
}

internal fun List<RecentSearch>.marketRecentSearches(): List<RecentSearch> {
    val assetGroups = mutableSetOf<String>()
    return filter { search ->
        when (search.type) {
            RecentSearchType.ASSET,
            RecentSearchType.MARKET,
            RecentSearchType.PERPETUAL,
            -> true
            else -> false
        }
    }.filter { search ->
        if (search.type != RecentSearchType.ASSET) {
            true
        } else {
            assetGroups.add(search.assetGroupKey())
        }
    }
}

private fun RecentSearch.assetGroupKey(): String =
    title
        ?.trim()
        ?.takeIf(String::isNotEmpty)
        ?.lowercase(Locale.ROOT)
        ?: primaryKey.orEmpty().trim().lowercase(Locale.ROOT)
