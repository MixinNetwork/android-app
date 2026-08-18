package one.mixin.android.ui.home.web3.trade.perps

import one.mixin.android.api.response.perps.PerpsMarket
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
    fun allMarketsSortByTradeVolumeScore() {
        val visibleMarkets =
            PerpsMarketListUiState(
                markets =
                    listOf(
                        perpsMarket("second", "crypto", tradeVolumeScore1D = 20),
                        perpsMarket("third", "stocks", tradeVolumeScore1D = 10),
                        perpsMarket("first", "crypto", tradeVolumeScore1D = 30),
                    ),
            ).visibleMarkets

        assertEquals(listOf("first", "second", "third"), visibleMarkets.map { it.marketId })
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
        assertSame(state, state.updateFavoriteMarketIds(emptySet()))
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
        assertFalse(emptyWatchlist.updateFavoriteMarketIds(setOf("btc")).isShowingRecommendations)
    }

    private fun perpsMarket(
        marketId: String,
        category: String,
        tradeVolumeScore1D: Int = 0,
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
        volume = "10",
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
