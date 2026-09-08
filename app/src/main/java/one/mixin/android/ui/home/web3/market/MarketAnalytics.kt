package one.mixin.android.ui.home.web3.market

import one.mixin.android.util.analytics.AnalyticsTracker

internal fun MarketTopTab.analyticsValue(): String =
    when (this) {
        MarketTopTab.WATCHLIST -> AnalyticsTracker.MarketsTab.WATCHLIST
        MarketTopTab.CRYPTO -> AnalyticsTracker.MarketsTab.CRYPTO
        MarketTopTab.PERPETUAL -> AnalyticsTracker.MarketsTab.PERPETUAL
        MarketTopTab.STOCK -> AnalyticsTracker.MarketsTab.STOCK
        MarketTopTab.INDICATOR -> AnalyticsTracker.MarketsTab.INDICATOR
    }

internal fun MarketSubTab?.analyticsValue(): String =
    when (this) {
        MarketSubTab.TRENDING -> AnalyticsTracker.MarketsTab.TRENDING
        MarketSubTab.TOP_GAINERS -> AnalyticsTracker.MarketsTab.TOP_GAINERS
        MarketSubTab.TOP_LOSERS -> AnalyticsTracker.MarketsTab.TOP_LOSERS
        MarketSubTab.ALL -> AnalyticsTracker.MarketsTab.ALL
        MarketSubTab.MEME -> AnalyticsTracker.MarketsTab.MEMES
        MarketSubTab.INDICES -> AnalyticsTracker.MarketsTab.INDICES
        MarketSubTab.COMMODITIES -> AnalyticsTracker.MarketsTab.COMMODITIES
        MarketSubTab.FOREX -> AnalyticsTracker.MarketsTab.FOREX
        MarketSubTab.CRYPTO -> AnalyticsTracker.MarketsTab.CRYPTO
        MarketSubTab.PERPETUAL -> AnalyticsTracker.MarketsTab.PERPETUAL
        MarketSubTab.FAVORITE,
        null,
        -> AnalyticsTracker.MarketsTab.WATCHLIST
    }

internal fun MarketPriceChangePeriod.analyticsValue(): String =
    when (this) {
        MarketPriceChangePeriod.TWENTY_FOUR_HOURS -> AnalyticsTracker.MarketsPeriod.TWENTY_FOUR_HOURS
        MarketPriceChangePeriod.SEVEN_DAYS -> AnalyticsTracker.MarketsPeriod.SEVEN_DAYS
    }

internal fun MarketSortDirection.analyticsValue(): String? =
    when (this) {
        MarketSortDirection.DESCENDING -> AnalyticsTracker.MarketsSortDirection.DESCENDING
        MarketSortDirection.ASCENDING -> AnalyticsTracker.MarketsSortDirection.ASCENDING
        MarketSortDirection.DEFAULT -> null
    }

internal fun MarketPageUiState.analyticsSortField(column: MarketSortColumn): String =
    when (column) {
        MarketSortColumn.VOLUME ->
            if (showsMarketCapColumn) {
                AnalyticsTracker.MarketsSortField.MARKET_CAP
            } else {
                AnalyticsTracker.MarketsSortField.VOLUME
            }
        MarketSortColumn.PRICE -> AnalyticsTracker.MarketsSortField.PRICE
        MarketSortColumn.CHANGE ->
            when (effectivePriceChangePeriod) {
                MarketPriceChangePeriod.TWENTY_FOUR_HOURS -> AnalyticsTracker.MarketsSortField.TWENTY_FOUR_HOURS
                MarketPriceChangePeriod.SEVEN_DAYS -> AnalyticsTracker.MarketsSortField.SEVEN_DAYS
            }
        MarketSortColumn.SCORE -> AnalyticsTracker.MarketsSortField.SCORE
    }
