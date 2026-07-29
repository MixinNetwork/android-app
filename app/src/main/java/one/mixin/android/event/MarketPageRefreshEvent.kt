package one.mixin.android.event

data class MarketPageRefreshEvent(
    val duration: String,
    val failedSources: Set<MarketPageDataSource>,
)

enum class MarketPageDataSource {
    SPOT_ALL,
    SPOT_FAVORITE,
    SPOT_FEATURED,
    SPOT_TRENDING,
    SPOT_TOP_GAINER,
    SPOT_TOP_LOSER,
    SPOT_STOCK,
    PERPETUAL_ALL,
    PERPETUAL_FAVORITE,
    PERPETUAL_FEATURED,
    GLOBAL,
}
