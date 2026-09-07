package one.mixin.android.ui.home.web3.trade.perps

import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.ui.home.web3.market.sortedByScoreAndVolumeDescending

private const val TOP_MOVERS_GROUP_SIZE = 4
private const val TRENDING_PREVIEW_SIZE = 3

fun List<PerpsMarket>.trendingPreview(): List<PerpsMarket> =
    sortedByScoreAndVolumeDescending().take(TRENDING_PREVIEW_SIZE)

fun List<PerpsMarket>.topMoversPreview(): List<PerpsMarket> {
    val topMarkets = sortedByDescending { it.changePercent() }
        .take(TOP_MOVERS_GROUP_SIZE)
    val topMarketIds = topMarkets.map { it.marketId }.toSet()
    val bottomMarkets = sortedBy { it.changePercent() }
        .filterNot { it.marketId in topMarketIds }
        .take(TOP_MOVERS_GROUP_SIZE)

    return topMarkets + bottomMarkets
}
