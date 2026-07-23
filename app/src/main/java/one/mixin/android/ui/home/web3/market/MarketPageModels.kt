package one.mixin.android.ui.home.web3.market

import one.mixin.android.api.response.perps.PerpsMarket
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
    TRENDING,
    TOP_GAINERS,
    TOP_LOSERS,
    ALL,
    CRYPTO,
    PERPETUAL,
}

enum class MarketPriceChangePeriod {
    TWENTY_FOUR_HOURS,
    SEVEN_DAYS,
}

enum class MarketSortColumn {
    VOLUME,
    PRICE,
    CHANGE,
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
    fun next(selectedColumn: MarketSortColumn): MarketSortState {
        if (column != selectedColumn) {
            return MarketSortState(selectedColumn, MarketSortDirection.DESCENDING)
        }
        return when (direction) {
            MarketSortDirection.DEFAULT -> MarketSortState(selectedColumn, MarketSortDirection.DESCENDING)
            MarketSortDirection.DESCENDING -> MarketSortState(selectedColumn, MarketSortDirection.ASCENDING)
            MarketSortDirection.ASCENDING -> MarketSortState()
        }
    }
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
    val favoriteCoinId: String?
    val isFavored: Boolean

    data class Spot(
        val market: MarketItem,
        val type: SpotMarketType,
    ) : MarketListEntry {
        override val stableId: String = "spot:${market.coinId}"
        override val favoriteCoinId: String = market.coinId
        override val isFavored: Boolean = market.isFavored == true
    }

    data class Perpetual(
        val market: PerpsMarket,
        val backingMarket: MarketItem?,
    ) : MarketListEntry {
        override val stableId: String = "perpetual:${market.marketId}"
        override val favoriteCoinId: String? = backingMarket?.coinId
        override val isFavored: Boolean = backingMarket?.isFavored == true
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
    val hasError: Boolean = false,
) {
    val selectedSubTab: MarketSubTab?
        get() = selectedSubTabs[selectedTopTab]

    val showsOnlyPerpetualMarkets: Boolean
        get() =
            selectedTopTab == MarketTopTab.PERPETUAL ||
                (selectedTopTab == MarketTopTab.WATCHLIST && selectedSubTab == MarketSubTab.PERPETUAL)

    val effectivePriceChangePeriod: MarketPriceChangePeriod
        get() =
            if (showsOnlyPerpetualMarkets) {
                MarketPriceChangePeriod.TWENTY_FOUR_HOURS
            } else {
                displaySettings.priceChangePeriod
            }
}

fun defaultMarketSubTabs(): Map<MarketTopTab, MarketSubTab> =
    mapOf(
        MarketTopTab.WATCHLIST to MarketSubTab.ALL,
        MarketTopTab.CRYPTO to MarketSubTab.TRENDING,
        MarketTopTab.PERPETUAL to MarketSubTab.TRENDING,
        MarketTopTab.STOCK to MarketSubTab.TRENDING,
    )

object MarketPageMapper {
    fun stockMarkets(
        markets: List<MarketItem>,
        subTab: MarketSubTab,
        period: MarketPriceChangePeriod,
    ): List<MarketItem> =
        when (subTab) {
            MarketSubTab.TOP_GAINERS -> markets.sortedByDescending { it.changePercent(period) ?: BigDecimal.ZERO }
            MarketSubTab.TOP_LOSERS -> markets.sortedBy { it.changePercent(period) ?: BigDecimal.ZERO }
            MarketSubTab.ALL -> markets.sortedBy { it.marketCapRank.toIntOrNull() ?: Int.MAX_VALUE }
            else -> markets
        }

    fun perpetualMarkets(
        markets: List<PerpsMarket>,
        subTab: MarketSubTab,
    ): List<PerpsMarket> =
        when (subTab) {
            MarketSubTab.TOP_GAINERS -> markets.sortedByDescending { it.changePercentValue() ?: BigDecimal.ZERO }
            MarketSubTab.TOP_LOSERS -> markets.sortedBy { it.changePercentValue() ?: BigDecimal.ZERO }
            else -> markets
        }

    fun watchlist(
        favorites: List<MarketItem>,
        stockCoinIds: Set<String>,
        perpetualById: Map<String, PerpsMarket>,
        subTab: MarketSubTab,
    ): List<MarketListEntry> {
        val favoriteMarkets = favorites.map { it.copy(isFavored = true) }
        return when (subTab) {
            MarketSubTab.CRYPTO ->
                favoriteMarkets
                    .filterNot { it.coinId in stockCoinIds }
                    .map { MarketListEntry.Spot(it, SpotMarketType.CRYPTO) }

            MarketSubTab.PERPETUAL ->
                favoriteMarkets.mapNotNull { favorite ->
                    favorite.perpsMarketId
                        ?.let(perpetualById::get)
                        ?.let { MarketListEntry.Perpetual(it, favorite) }
                }

            else ->
                buildList {
                    favoriteMarkets.forEach { favorite ->
                        favorite.perpsMarketId
                            ?.let(perpetualById::get)
                            ?.let { add(MarketListEntry.Perpetual(it, favorite)) }
                        add(
                            MarketListEntry.Spot(
                                market = favorite,
                                type = if (favorite.coinId in stockCoinIds) SpotMarketType.STOCK else SpotMarketType.CRYPTO,
                            )
                        )
                    }
                }
        }
    }

    fun applySort(
        entries: List<MarketListEntry>,
        sortState: MarketSortState,
        period: MarketPriceChangePeriod,
    ): List<MarketListEntry> {
        val column = sortState.column ?: return entries
        if (sortState.direction == MarketSortDirection.DEFAULT) return entries
        val comparator = compareBy<MarketListEntry> { entry ->
            when (column) {
                MarketSortColumn.VOLUME -> entry.volume()
                MarketSortColumn.PRICE -> entry.price()
                MarketSortColumn.CHANGE -> entry.changePercent(period)
            } ?: BigDecimal.ZERO
        }
        return if (sortState.direction == MarketSortDirection.ASCENDING) {
            entries.sortedWith(comparator)
        } else {
            entries.sortedWith(comparator.reversed())
        }
    }
}

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

fun MarketListEntry.volume(): BigDecimal? =
    when (this) {
        is MarketListEntry.Spot -> market.totalVolume.toBigDecimalOrNull()
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
