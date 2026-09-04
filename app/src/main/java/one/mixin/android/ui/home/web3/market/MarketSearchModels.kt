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

internal fun <T> List<T>.sortedForMarketSearch(
    query: String,
    symbol: (T) -> String,
    name: (T) -> String,
    volume: (T) -> String,
): List<T> {
    val normalizedQuery = query.trim()
    return sortedWith(
        compareByDescending<T> { symbol(it).equals(normalizedQuery, ignoreCase = true) }
            .thenByDescending { name(it).equals(normalizedQuery, ignoreCase = true) }
            .thenByDescending { volume(it).toBigDecimalOrNull() ?: BigDecimal.ZERO }
            .thenBy { symbol(it).lowercase(Locale.ROOT) }
            .thenBy { name(it).lowercase(Locale.ROOT) },
    )
}

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

internal data class MarketRecentSearch(
    val search: RecentSearch,
    val change: BigDecimal? = null,
)

internal const val MAX_MARKET_RECENT_SEARCHES = 6

internal fun List<RecentSearch>.marketRecentSearches(): List<RecentSearch> {
    return filter { search ->
        search.type == RecentSearchType.MARKET || search.type == RecentSearchType.PERPETUAL
    }
}

internal fun List<RecentSearch>.addMarketRecentSearch(search: RecentSearch): List<RecentSearch> {
    if (search.type != RecentSearchType.MARKET && search.type != RecentSearchType.PERPETUAL) {
        return marketRecentSearches()
    }
    val marketSearches = marketRecentSearches()
    return buildList {
        add(search)
        addAll(
            marketSearches
                .filterNot { it.isSameMarketRecentSearch(search) }
                .take(MAX_MARKET_RECENT_SEARCHES - 1),
        )
    }
}

private fun RecentSearch.isSameMarketRecentSearch(other: RecentSearch): Boolean {
    if (type != other.type) return false
    return if (!primaryKey.isNullOrBlank() && !other.primaryKey.isNullOrBlank()) {
        primaryKey == other.primaryKey
    } else {
        title.equals(other.title, ignoreCase = true) &&
            subTitle.equals(other.subTitle, ignoreCase = true)
    }
}
