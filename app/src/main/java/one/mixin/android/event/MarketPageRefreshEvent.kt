package one.mixin.android.event

data class MarketPageRefreshEvent(
    val duration: String,
    val refreshedSources: Set<MarketPageDataSource>,
    val failedSources: Set<MarketPageDataSource>,
) {
    val isFullRefresh: Boolean
        get() = refreshedSources == ALL_MARKET_PAGE_DATA_SOURCES
}

enum class MarketPageDataSource {
    SPOT_ALL,
    SPOT_FAVORITE,
    SPOT_FEATURED,
    SPOT_TRENDING,
    SPOT_TOP_GAINER,
    SPOT_TOP_LOSER,
    PERPETUAL_ALL,
    PERPETUAL_FAVORITE,
    PERPETUAL_FEATURED,
    GLOBAL,
}

internal val ALL_MARKET_PAGE_DATA_SOURCES = MarketPageDataSource.entries.toSet()
