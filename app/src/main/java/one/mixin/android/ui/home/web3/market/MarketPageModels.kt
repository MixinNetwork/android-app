package one.mixin.android.ui.home.web3.market

import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.event.MarketPageDataSource
import one.mixin.android.vo.market.GlobalMarket
import one.mixin.android.vo.market.MarketItem
import java.math.BigDecimal

enum class MarketTopTab {
    WATCHLIST,
    CRYPTO,
    PERPETUAL,
    STOCK,
    INDICATOR,
}

enum class MarketSubTab {
    FAVORITE,
    TRENDING,
    TOP_GAINERS,
    TOP_LOSERS,
    ALL,
    CRYPTO,
    PERPETUAL,
    INDICES,
    COMMODITIES,
    FOREX,
    MEME,
}

internal enum class PerpsMarketCategoryKey(
    val databaseValue: String,
    private val aliases: Set<String> = emptySet(),
) {
    CRYPTO("crypto"),
    STOCKS("stocks", setOf("stock")),
    MEME("memes", setOf("meme")),
    INDICES("indices", setOf("index")),
    COMMODITIES("commodities", setOf("commodity")),
    FOREX("forex", setOf("fx")),
    ;

    fun matches(value: String): Boolean =
        databaseValue.equals(value, ignoreCase = true) ||
            aliases.any { alias -> alias.equals(value, ignoreCase = true) }
}

enum class MarketPriceChangePeriod {
    TWENTY_FOUR_HOURS,
    SEVEN_DAYS,
}

enum class MarketSortColumn {
    VOLUME,
    PRICE,
    CHANGE,
    SCORE,
}

enum class MarketSortDirection {
    DEFAULT,
    ASCENDING,
    DESCENDING,
}

data class MarketSortState(
    val column: MarketSortColumn? = null,
    val direction: MarketSortDirection = MarketSortDirection.DEFAULT,
) {
    fun next(
        selectedColumn: MarketSortColumn,
        defaultState: MarketSortState = MarketSortState(),
    ): MarketSortState {
        if (this == defaultState && column == selectedColumn) {
            return MarketSortState(
                selectedColumn,
                if (direction == MarketSortDirection.ASCENDING) {
                    MarketSortDirection.DESCENDING
                } else {
                    MarketSortDirection.ASCENDING
                },
            )
        }
        if (column != selectedColumn) {
            return MarketSortState(selectedColumn, MarketSortDirection.DESCENDING)
        }
        return when (direction) {
            MarketSortDirection.DEFAULT -> MarketSortState(selectedColumn, MarketSortDirection.DESCENDING)
            MarketSortDirection.DESCENDING -> MarketSortState(selectedColumn, MarketSortDirection.ASCENDING)
            MarketSortDirection.ASCENDING -> defaultState
        }
    }
}

fun defaultMarketSortState(
    topTab: MarketTopTab,
    subTab: MarketSubTab?,
): MarketSortState =
    when {
        subTab == MarketSubTab.TOP_GAINERS ->
            MarketSortState(MarketSortColumn.CHANGE, MarketSortDirection.DESCENDING)
        subTab == MarketSubTab.TOP_LOSERS ->
            MarketSortState(MarketSortColumn.CHANGE, MarketSortDirection.ASCENDING)
        isScoreOrderingAvailable(topTab, subTab) -> scoreMarketSortState()
        else -> MarketSortState()
    }

internal fun scoreMarketSortState() =
    MarketSortState(MarketSortColumn.SCORE, MarketSortDirection.DESCENDING)

internal fun isScoreOrderingAvailable(
    topTab: MarketTopTab,
    subTab: MarketSubTab?,
): Boolean =
    when (topTab) {
        MarketTopTab.PERPETUAL ->
            subTab == MarketSubTab.TRENDING ||
                subTab == MarketSubTab.MEME ||
                subTab == MarketSubTab.INDICES ||
                subTab == MarketSubTab.COMMODITIES ||
                subTab == MarketSubTab.FOREX
        MarketTopTab.STOCK -> subTab == MarketSubTab.PERPETUAL
        else -> false
    }

data class MarketDisplaySettings(
    val quoteColorReversed: Boolean = false,
    val priceChangePeriod: MarketPriceChangePeriod = MarketPriceChangePeriod.SEVEN_DAYS,
)

enum class SpotMarketType {
    CRYPTO,
    STOCK,
}

sealed interface MarketListEntry {
    val stableId: String
    val favoriteId: String
    val isFavored: Boolean

    data class Spot(
        val market: MarketItem,
        val type: SpotMarketType,
    ) : MarketListEntry {
        override val stableId: String = "spot:${market.coinId}"
        override val favoriteId: String = market.coinId
        override val isFavored: Boolean = market.isFavored == true
    }

    data class Perpetual(
        val market: PerpsMarket,
        override val isFavored: Boolean,
    ) : MarketListEntry {
        override val stableId: String = "perpetual:${market.marketId}"
        override val favoriteId: String = market.marketId
    }
}

data class MarketPageUiState(
    val selectedTopTab: MarketTopTab = MarketTopTab.CRYPTO,
    val selectedSubTabs: Map<MarketTopTab, MarketSubTab> = defaultMarketSubTabs(),
    val entries: List<MarketListEntry> = emptyList(),
    val displaySettings: MarketDisplaySettings = MarketDisplaySettings(),
    val sortState: MarketSortState = MarketSortState(),
    val indicator: GlobalMarket? = null,
    val isLoading: Boolean = true,
    val hasLoadedLocalData: Boolean = false,
    val hasError: Boolean = false,
    val isShowingRecommendations: Boolean = false,
    val isAddingRecommendations: Boolean = false,
) {
    val selectedSubTab: MarketSubTab?
        get() = selectedSubTabs[selectedTopTab]

    val showsOnlyPerpetualMarkets: Boolean
        get() =
            selectedTopTab == MarketTopTab.PERPETUAL ||
                (selectedTopTab == MarketTopTab.WATCHLIST && selectedSubTab == MarketSubTab.PERPETUAL) ||
                (selectedTopTab == MarketTopTab.STOCK && selectedSubTab == MarketSubTab.PERPETUAL)

    val showsMarketCapColumn: Boolean
        get() = selectedTopTab == MarketTopTab.CRYPTO && selectedSubTab == MarketSubTab.ALL

    val effectivePriceChangePeriod: MarketPriceChangePeriod
        get() =
            if (showsOnlyPerpetualMarkets) {
                MarketPriceChangePeriod.TWENTY_FOUR_HOURS
            } else {
                displaySettings.priceChangePeriod
            }

    val showsMarketLoading: Boolean
        get() = isLoading && hasLoadedLocalData && entries.isEmpty()
}

fun defaultMarketSubTabs(): Map<MarketTopTab, MarketSubTab> =
    mapOf(
        MarketTopTab.WATCHLIST to MarketSubTab.CRYPTO,
        MarketTopTab.CRYPTO to MarketSubTab.TRENDING,
        MarketTopTab.PERPETUAL to MarketSubTab.TRENDING,
        MarketTopTab.STOCK to MarketSubTab.CRYPTO,
    )

fun marketSubTabs(topTab: MarketTopTab): List<MarketSubTab> =
    when (topTab) {
        MarketTopTab.WATCHLIST -> listOf(MarketSubTab.CRYPTO, MarketSubTab.PERPETUAL)
        MarketTopTab.CRYPTO ->
            listOf(
                MarketSubTab.FAVORITE,
                MarketSubTab.TRENDING,
                MarketSubTab.TOP_GAINERS,
                MarketSubTab.TOP_LOSERS,
                MarketSubTab.ALL,
            )
        MarketTopTab.PERPETUAL ->
            listOf(
                MarketSubTab.FAVORITE,
                MarketSubTab.TRENDING,
                MarketSubTab.TOP_GAINERS,
                MarketSubTab.TOP_LOSERS,
                MarketSubTab.MEME,
                MarketSubTab.INDICES,
                MarketSubTab.COMMODITIES,
                MarketSubTab.FOREX,
            )
        MarketTopTab.STOCK -> listOf(MarketSubTab.CRYPTO, MarketSubTab.PERPETUAL)
        MarketTopTab.INDICATOR -> emptyList()
    }

internal fun marketPageRefreshSources(
    topTab: MarketTopTab,
    subTab: MarketSubTab?,
): Set<MarketPageDataSource> =
    when (topTab) {
        MarketTopTab.WATCHLIST ->
            if (subTab == MarketSubTab.PERPETUAL) {
                setOf(
                    MarketPageDataSource.PERPETUAL_FAVORITE,
                    MarketPageDataSource.PERPETUAL_FEATURED,
                )
            } else {
                setOf(
                    MarketPageDataSource.SPOT_FAVORITE,
                    MarketPageDataSource.SPOT_FEATURED,
                )
            }

        MarketTopTab.CRYPTO ->
            when (subTab) {
                MarketSubTab.FAVORITE ->
                    setOf(
                        MarketPageDataSource.SPOT_FAVORITE,
                        MarketPageDataSource.SPOT_FEATURED,
                    )
                MarketSubTab.TOP_GAINERS -> setOf(MarketPageDataSource.SPOT_TOP_GAINER)
                MarketSubTab.TOP_LOSERS -> setOf(MarketPageDataSource.SPOT_TOP_LOSER)
                MarketSubTab.ALL ->
                    setOf(
                        MarketPageDataSource.SPOT_ALL,
                        MarketPageDataSource.SPOT_STOCK,
                    )
                else -> setOf(MarketPageDataSource.SPOT_TRENDING)
            }

        MarketTopTab.PERPETUAL ->
            if (subTab == MarketSubTab.FAVORITE) {
                setOf(
                    MarketPageDataSource.PERPETUAL_FAVORITE,
                    MarketPageDataSource.PERPETUAL_FEATURED,
                )
            } else {
                setOf(MarketPageDataSource.PERPETUAL_ALL)
            }

        MarketTopTab.STOCK ->
            if (subTab == MarketSubTab.PERPETUAL) {
                setOf(MarketPageDataSource.PERPETUAL_ALL)
            } else {
                setOf(MarketPageDataSource.SPOT_STOCK)
            }

        MarketTopTab.INDICATOR -> setOf(MarketPageDataSource.GLOBAL)
    }

object MarketPageMapper {
    fun spotMarkets(
        markets: List<MarketItem>,
        fallbackMarkets: List<MarketItem> = emptyList(),
    ): List<MarketItem> = markets.ifEmpty { fallbackMarkets }

    fun perpetualMarkets(
        markets: List<PerpsMarket>,
        subTab: MarketSubTab,
    ): List<PerpsMarket> =
        when (subTab) {
            MarketSubTab.TRENDING -> markets.sortedByScoreAndVolumeDescending()
            MarketSubTab.TOP_GAINERS -> markets.sortedByDescending { it.changePercentValue() ?: BigDecimal.ZERO }
            MarketSubTab.TOP_LOSERS -> markets.sortedBy { it.changePercentValue() ?: BigDecimal.ZERO }
            MarketSubTab.INDICES -> markets.filterByCategory(PerpsMarketCategoryKey.INDICES).sortedByScoreAndVolumeDescending()
            MarketSubTab.COMMODITIES -> markets.filterByCategory(PerpsMarketCategoryKey.COMMODITIES).sortedByScoreAndVolumeDescending()
            MarketSubTab.FOREX -> markets.filterByCategory(PerpsMarketCategoryKey.FOREX).sortedByScoreAndVolumeDescending()
            MarketSubTab.MEME -> markets.filterByCategory(PerpsMarketCategoryKey.MEME).sortedByScoreAndVolumeDescending()
            else -> markets
        }

    fun perpetualStockMarkets(markets: List<PerpsMarket>): List<PerpsMarket> =
        markets.filterByCategory(PerpsMarketCategoryKey.STOCKS).sortedByScoreAndVolumeDescending()

    private fun List<PerpsMarket>.filterByCategory(category: PerpsMarketCategoryKey): List<PerpsMarket> =
        filter { market -> category.matches(market.category) }

    fun watchlist(
        spotFavorites: List<MarketItem>,
        perpetualFavorites: List<PerpsMarket>,
        stockCoinIds: Set<String>,
        subTab: MarketSubTab,
        spotFeatured: List<MarketItem> = emptyList(),
        perpetualFeatured: List<PerpsMarket> = emptyList(),
    ): List<MarketListEntry> {
        return when (subTab) {
            MarketSubTab.PERPETUAL ->
                if (perpetualFavorites.isNotEmpty()) {
                    perpetualFavorites.map { MarketListEntry.Perpetual(it, true) }
                } else {
                    perpetualFeatured.map { MarketListEntry.Perpetual(it, false) }
                }

            else -> {
                val markets =
                    if (spotFavorites.isNotEmpty()) {
                        spotFavorites.map { it.copy(isFavored = true) }
                    } else {
                        spotFeatured.map { it.copy(isFavored = false) }
                    }
                markets
                    .map { market ->
                        MarketListEntry.Spot(
                            market = market,
                            type =
                                if (market.coinId in stockCoinIds) {
                                    SpotMarketType.STOCK
                                } else {
                                    SpotMarketType.CRYPTO
                                },
                        )
                    }
            }
        }
    }

    fun applySort(
        entries: List<MarketListEntry>,
        sortState: MarketSortState,
        period: MarketPriceChangePeriod,
        useMarketCapForSpot: Boolean,
    ): List<MarketListEntry> {
        val column = sortState.column ?: return entries
        if (sortState.direction == MarketSortDirection.DEFAULT) return entries
        val comparator = compareBy<MarketListEntry> { entry ->
            when (column) {
                MarketSortColumn.VOLUME -> entry.volume(useMarketCapForSpot)
                MarketSortColumn.PRICE -> entry.price()
                MarketSortColumn.CHANGE -> entry.changePercent(period)
                MarketSortColumn.SCORE -> entry.score()
            } ?: BigDecimal.ZERO
        }.let { baseComparator ->
            if (column == MarketSortColumn.SCORE) {
                baseComparator.thenBy { entry -> entry.volume(useMarketCapForSpot) ?: BigDecimal.ZERO }
            } else {
                baseComparator
            }
        }
        return if (sortState.direction == MarketSortDirection.ASCENDING) {
            entries.sortedWith(comparator)
        } else {
            entries.sortedWith(comparator.reversed())
        }
    }
}

internal fun List<PerpsMarket>.sortedByScoreAndVolumeDescending(): List<PerpsMarket> =
    sortedWith(
        compareByDescending<PerpsMarket>(PerpsMarket::tradeVolumeScore1D)
            .thenByDescending { market -> market.volume.toBigDecimalOrNull() ?: BigDecimal.ZERO },
    )

fun MarketItem.changePercent(period: MarketPriceChangePeriod): BigDecimal? =
    when (period) {
        MarketPriceChangePeriod.TWENTY_FOUR_HOURS -> priceChangePercentage24H
        MarketPriceChangePeriod.SEVEN_DAYS -> priceChangePercentage7D
    }.toBigDecimalOrNull()

fun MarketItem.sparkline(period: MarketPriceChangePeriod): String =
    when (period) {
        MarketPriceChangePeriod.TWENTY_FOUR_HOURS -> sparklineIn24
        MarketPriceChangePeriod.SEVEN_DAYS -> sparklineIn7d
    }

fun PerpsMarket.changePercentValue(): BigDecimal? {
    return change.toBigDecimalOrNull()?.multiply(BigDecimal(100))
}

fun MarketListEntry.volume(useMarketCapForSpot: Boolean): BigDecimal? =
    when (this) {
        is MarketListEntry.Spot ->
            if (useMarketCapForSpot) {
                market.marketCap.toBigDecimalOrNull()
            } else {
                market.totalVolume.toBigDecimalOrNull()
            }
        is MarketListEntry.Perpetual -> market.volume.toBigDecimalOrNull()
    }

fun MarketListEntry.price(): BigDecimal? =
    when (this) {
        is MarketListEntry.Spot -> market.currentPrice.toBigDecimalOrNull()
        is MarketListEntry.Perpetual -> market.last.toBigDecimalOrNull()
    }

fun MarketListEntry.changePercent(period: MarketPriceChangePeriod): BigDecimal? =
    when (this) {
        is MarketListEntry.Spot -> market.changePercent(period)
        is MarketListEntry.Perpetual -> market.changePercentValue()
    }

fun MarketListEntry.score(): BigDecimal? =
    when (this) {
        is MarketListEntry.Spot -> null
        is MarketListEntry.Perpetual -> market.tradeVolumeScore1D.toBigDecimal()
    }
