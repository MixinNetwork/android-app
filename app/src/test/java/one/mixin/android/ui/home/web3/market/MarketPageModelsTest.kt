package one.mixin.android.ui.home.web3.market

import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.vo.market.MarketItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketPageModelsTest {
    @Test
    fun defaultSelectionStartsAtExpectedSubTabs() {
        val defaults = defaultMarketSubTabs()

        assertEquals(MarketSubTab.CRYPTO, defaults[MarketTopTab.WATCHLIST])
        assertEquals(MarketSubTab.TRENDING, defaults[MarketTopTab.CRYPTO])
        assertEquals(MarketSubTab.TRENDING, defaults[MarketTopTab.PERPETUAL])
        assertEquals(MarketSubTab.TRENDING, defaults[MarketTopTab.STOCK])
    }

    @Test
    fun stockGainersUseSelectedPriceChangePeriod() {
        val first = market(coinId = "first", change24h = "10", change7d = "-5")
        val second = market(coinId = "second", change24h = "-2", change7d = "20")

        val result =
            MarketPageMapper.stockMarkets(
                markets = listOf(first, second),
                subTab = MarketSubTab.TOP_GAINERS,
                period = MarketPriceChangePeriod.SEVEN_DAYS,
            )

        assertEquals(listOf("second", "first"), result.map { it.coinId })
    }

    @Test
    fun watchlistLegacyAllDoesNotMixMarketTypes() {
        val crypto = market(coinId = "btc", perpsMarketId = "btc-perp", favored = true)
        val stock = market(coinId = "hood", favored = true)
        val perpetual = perpsMarket(marketId = "btc-perp")

        val result =
            MarketPageMapper.watchlist(
                spotFavorites = listOf(crypto, stock),
                perpetualFavorites = listOf(perpetual),
                stockCoinIds = setOf("hood"),
                subTab = MarketSubTab.ALL,
            )

        assertEquals(listOf("spot:btc"), result.map { it.stableId })
    }

    @Test
    fun watchlistPerpetualUsesIndependentFavorites() {
        val spotFavorite = market(coinId = "eth", perpsMarketId = "eth-perp", favored = true)
        val perpetualFavorite = perpsMarket("btc-perp")

        val result =
            MarketPageMapper.watchlist(
                spotFavorites = listOf(spotFavorite),
                perpetualFavorites = listOf(perpetualFavorite),
                stockCoinIds = emptySet(),
                subTab = MarketSubTab.PERPETUAL,
            )

        assertEquals(listOf("perpetual:btc-perp"), result.map { it.stableId })
        assertTrue(result.single().isFavored)
    }

    @Test
    fun perpetualChangeAlwaysUsesTwentyFourHourData() {
        val market = perpsMarket(marketId = "btc-perp", change = "0.12")
        val entry = MarketListEntry.Perpetual(market, false)

        assertEquals("12.00", entry.changePercent(MarketPriceChangePeriod.SEVEN_DAYS)?.toPlainString())
        assertEquals("12.00", entry.changePercent(MarketPriceChangePeriod.TWENTY_FOUR_HOURS)?.toPlainString())
    }

    @Test
    fun perpetualOnlySelectionForcesTwentyFourHourDisplay() {
        val state =
            MarketPageUiState(
                selectedTopTab = MarketTopTab.PERPETUAL,
                displaySettings =
                    MarketDisplaySettings(
                        priceChangePeriod = MarketPriceChangePeriod.SEVEN_DAYS,
                    ),
            )

        assertTrue(state.showsOnlyPerpetualMarkets)
        assertEquals(MarketPriceChangePeriod.TWENTY_FOUR_HOURS, state.effectivePriceChangePeriod)
    }

    @Test
    fun emptyPerpetualWatchlistShowsApiRecommendations() {
        val recommendation = MarketListEntry.Perpetual(perpsMarket("btc-perp"), false)
        val state =
            MarketPageUiState(
                selectedTopTab = MarketTopTab.WATCHLIST,
                selectedSubTabs = defaultMarketSubTabs() + (MarketTopTab.WATCHLIST to MarketSubTab.PERPETUAL),
                perpetualRecommendations = listOf(recommendation),
                isLoading = false,
            )

        assertTrue(state.showsPerpetualRecommendations)
        assertFalse(state.copy(entries = listOf(recommendation)).showsPerpetualRecommendations)
        assertFalse(
            state.copy(
                selectedSubTabs = defaultMarketSubTabs() + (MarketTopTab.WATCHLIST to MarketSubTab.CRYPTO),
            ).showsPerpetualRecommendations,
        )
    }

    @Test
    fun sortHeaderCyclesDescendingAscendingAndDefault() {
        val descending = MarketSortState().next(MarketSortColumn.PRICE)
        val ascending = descending.next(MarketSortColumn.PRICE)
        val reset = ascending.next(MarketSortColumn.PRICE)

        assertEquals(MarketSortDirection.DESCENDING, descending.direction)
        assertEquals(MarketSortDirection.ASCENDING, ascending.direction)
        assertEquals(MarketSortState(), reset)
    }

    private fun market(
        coinId: String,
        change24h: String = "0",
        change7d: String = "0",
        perpsMarketId: String? = null,
        favored: Boolean = false,
    ) = MarketItem(
        coinId = coinId,
        name = coinId,
        symbol = coinId.uppercase(),
        iconUrl = "",
        currentPrice = "1",
        marketCap = "100",
        marketCapRank = "1",
        totalVolume = "10",
        high24h = "1",
        low24h = "1",
        priceChange24h = "0",
        priceChangePercentage1H = "0",
        priceChangePercentage24H = change24h,
        priceChangePercentage7D = change7d,
        priceChangePercentage30D = "0",
        marketCapChange24h = "0",
        marketCapChangePercentage24h = "0",
        circulatingSupply = "0",
        totalSupply = "0",
        maxSupply = "0",
        ath = "0",
        athChangePercentage = "0",
        athDate = "",
        atl = "0",
        atlChangePercentage = "0",
        atlDate = "",
        assetIds = emptyList(),
        sparklineIn7d = "",
        sparklineIn24 = "",
        isFavored = favored,
        perpsMarketId = perpsMarketId,
    )

    private fun perpsMarket(
        marketId: String,
        change: String = "0",
    ) = PerpsMarket(
        marketId = marketId,
        displaySymbol = marketId,
        tokenSymbol = "BTC",
        quoteSymbol = "USD",
        markPrice = "1",
        leverage = 10,
        iconUrl = "",
        fundingRate = "0",
        minAmount = "0",
        maxAmount = "0",
        last = "1",
        volume = "10",
        high = "1",
        low = "1",
        open = "1",
        change = change,
        bidPrice = "1",
        askPrice = "1",
        createdAt = "",
        updatedAt = "",
    )
}
