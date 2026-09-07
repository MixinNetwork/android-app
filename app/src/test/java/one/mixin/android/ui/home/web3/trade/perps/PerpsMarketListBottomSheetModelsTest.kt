package one.mixin.android.ui.home.web3.trade.perps

import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.ui.home.web3.market.MarketSortColumn
import one.mixin.android.ui.home.web3.market.MarketSortDirection
import one.mixin.android.ui.home.web3.market.MarketSortState
import one.mixin.android.ui.home.web3.market.scoreMarketSortState
import one.mixin.android.ui.home.web3.widget.MarketSort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PerpsMarketListBottomSheetModelsTest {
    @Test
    fun resolvesLegacyCategoryAliasesIgnoringCase() {
        assertEquals(PerpsMarketCategory.STOCKS, PerpsMarketCategory.fromInitialCategory("STOCK"))
        assertEquals(PerpsMarketCategory.INDICES, PerpsMarketCategory.fromInitialCategory("index"))
        assertEquals(PerpsMarketCategory.COMMODITIES, PerpsMarketCategory.fromInitialCategory("Commodity"))
        assertEquals(PerpsMarketCategory.FOREX, PerpsMarketCategory.fromInitialCategory("fx"))
        assertEquals(PerpsMarketCategory.MEME, PerpsMarketCategory.fromInitialCategory("MEME"))
    }

    @Test
    fun filtersLegacyCategoryAliasesIgnoringCase() {
        val markets =
            listOf(
                perpsMarket("stock", "STOCK"),
                perpsMarket("stocks", "stocks"),
                perpsMarket("crypto", "crypto"),
            )

        val visibleMarkets =
            PerpsMarketListUiState(
                selectedCategory = PerpsMarketCategory.STOCKS,
                markets = markets,
            ).visibleMarkets

        assertEquals(setOf("stock", "stocks"), visibleMarkets.mapTo(mutableSetOf()) { it.marketId })
    }

    @Test
    fun allMarketsSortByTradeVolume() {
        val visibleMarkets =
            PerpsMarketListUiState(
                markets =
                    listOf(
                        perpsMarket("second-low-volume", "crypto", tradeVolumeScore1D = 20, volume = "10"),
                        perpsMarket("second-high-volume", "stocks", tradeVolumeScore1D = 20, volume = "100"),
                        perpsMarket("third", "stocks", tradeVolumeScore1D = 10, volume = "200"),
                        perpsMarket("first", "crypto", tradeVolumeScore1D = 30, volume = "1"),
                    ),
            ).visibleMarkets

        assertEquals(
            listOf("third", "second-high-volume", "second-low-volume", "first"),
            visibleMarkets.map { it.marketId },
        )
    }

    @Test
    fun trendingOpeningUsesScoreOrderWithoutChangingCategoryDefaults() {
        val state =
            PerpsMarketListUiState.initial(
                initialCategory = null,
                initialSort = MarketSort.RANK_DESCENDING,
                quoteColorReversed = false,
            ).updateMarkets(
                listOf(
                    perpsMarket("volume-first", "crypto", tradeVolumeScore1D = 10, volume = "100"),
                    perpsMarket("trending-first", "stocks", tradeVolumeScore1D = 30, volume = "1"),
                ),
            )

        assertEquals(scoreMarketSortState(), state.sortState)
        assertEquals(listOf("trending-first", "volume-first"), state.visibleMarkets.map { it.marketId })
        assertEquals(
            defaultPerpsMarketSortState(PerpsMarketCategory.CRYPTO),
            state.selectCategory(PerpsMarketCategory.CRYPTO).sortState,
        )
    }

    @Test
    fun allCategoryHeaderCyclesBackToVolumeDefault() {
        val markets =
            listOf(
                perpsMarket("second", "crypto", tradeVolumeScore1D = 20, volume = "300"),
                perpsMarket("third", "stocks", tradeVolumeScore1D = 10, volume = "200"),
                perpsMarket("first", "crypto", tradeVolumeScore1D = 30, volume = "100"),
            )
        val ascending = PerpsMarketListUiState(markets = markets).selectSort(MarketSortColumn.VOLUME)
        val default = ascending.selectSort(MarketSortColumn.VOLUME)

        assertEquals(MarketSortState(MarketSortColumn.VOLUME, MarketSortDirection.ASCENDING), ascending.sortState)
        assertEquals(defaultPerpsMarketSortState(PerpsMarketCategory.ALL), default.sortState)
        assertEquals(listOf("second", "third", "first"), default.visibleMarkets.map { it.marketId })
        assertEquals(
            defaultPerpsMarketSortState(PerpsMarketCategory.CRYPTO),
            default.selectCategory(PerpsMarketCategory.CRYPTO).sortState,
        )
    }

    @Test
    fun marketCategoriesUseVolumeDefaultExceptWatchlist() {
        PerpsMarketCategory.entries
            .filterNot { it == PerpsMarketCategory.WATCHLIST }
            .forEach { category ->
                assertEquals(
                    MarketSortState(MarketSortColumn.VOLUME, MarketSortDirection.DESCENDING),
                    defaultPerpsMarketSortState(category),
                )
            }
        assertEquals(MarketSortState(), defaultPerpsMarketSortState(PerpsMarketCategory.WATCHLIST))
    }

    @Test
    fun cachesDerivedMarketListsForEachStateInstance() {
        val state =
            PerpsMarketListUiState(
                markets = listOf(perpsMarket("btc", "crypto")),
                featuredMarkets = listOf(perpsMarket("eth", "crypto")),
            )

        assertSame(state.visibleMarkets, state.visibleMarkets)
        assertSame(state.recommendations, state.recommendations)
    }

    @Test
    fun derivedListsAreRecomputedForUpdatedState() {
        val state =
            PerpsMarketListUiState(
                markets = listOf(perpsMarket("btc", "crypto")),
                featuredMarkets = listOf(perpsMarket("eth", "crypto")),
            )
        val originalVisibleMarkets = state.visibleMarkets
        val updated = state.updateMarkets(listOf(perpsMarket("sol", "crypto")))

        assertSame(originalVisibleMarkets, state.visibleMarkets)
        assertSame(updated.visibleMarkets, updated.visibleMarkets)
        assertEquals(listOf("sol"), updated.visibleMarkets.map { it.marketId })
    }

    @Test
    fun unchangedInputsKeepCurrentStateInstance() {
        val markets = listOf(perpsMarket("btc", "crypto"))
        val featuredMarkets = listOf(perpsMarket("eth", "crypto"))
        val state = PerpsMarketListUiState(markets = markets, featuredMarkets = featuredMarkets)

        assertSame(state, state.updateMarkets(markets.toList()))
        assertSame(state, state.updateFeaturedMarkets(featuredMarkets.toList()))
        assertSame(state, state.updateFavoriteMarketIds(emptyList()))
    }

    @Test
    fun recommendationsOnlyShowForBlankEmptyWatchlist() {
        val featuredMarket = perpsMarket("eth", "crypto")
        val emptyWatchlist =
            PerpsMarketListUiState(
                selectedCategory = PerpsMarketCategory.WATCHLIST,
                markets = listOf(perpsMarket("btc", "crypto")),
                featuredMarkets = listOf(featuredMarket),
            )

        assertTrue(emptyWatchlist.isShowingRecommendations)
        assertFalse(emptyWatchlist.updateQuery("btc").isShowingRecommendations)
        assertFalse(emptyWatchlist.updateFavoriteMarketIds(listOf("btc")).isShowingRecommendations)
    }

    @Test
    fun watchlistOrdersMarketsByNewestAddition() {
        val visibleMarkets =
            PerpsMarketListUiState(
                selectedCategory = PerpsMarketCategory.WATCHLIST,
                sortState = defaultPerpsMarketSortState(PerpsMarketCategory.WATCHLIST),
                markets =
                    listOf(
                        perpsMarket("old", "crypto"),
                        perpsMarket("new", "stocks"),
                        perpsMarket("middle", "crypto"),
                    ),
                favoriteMarketIds = listOf("new", "middle", "old"),
            ).visibleMarkets

        assertEquals(listOf("new", "middle", "old"), visibleMarkets.map { it.marketId })
    }

    private fun perpsMarket(
        marketId: String,
        category: String,
        tradeVolumeScore1D: Int = 0,
        volume: String = "10",
    ) = PerpsMarket(
        marketId = marketId,
        displaySymbol = marketId,
        tokenSymbol = marketId.uppercase(),
        quoteSymbol = "USD",
        markPrice = "1",
        leverage = 10,
        iconUrl = "",
        category = category,
        fundingRate = "0",
        minAmount = "0",
        maxAmount = "0",
        last = "1",
        volume = volume,
        tradeVolumeScore1D = tradeVolumeScore1D,
        high = "1",
        low = "1",
        open = "1",
        change = "0",
        bidPrice = "1",
        askPrice = "1",
        createdAt = "",
        updatedAt = "",
    )
}
