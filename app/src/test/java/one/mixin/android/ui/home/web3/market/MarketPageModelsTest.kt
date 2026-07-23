package one.mixin.android.ui.home.web3.market

import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.vo.market.MarketItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketPageModelsTest {
    @Test
    fun defaultSelectionStartsAtExpectedSubTabs() {
        val defaults = defaultMarketSubTabs()

        assertEquals(MarketSubTab.ALL, defaults[MarketTopTab.WATCHLIST])
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
    fun watchlistAllAddsLinkedPerpetualBeforeSpotAndKeepsStocks() {
        val crypto = market(coinId = "btc", perpsMarketId = "btc-perp", favored = true)
        val stock = market(coinId = "hood", favored = true)
        val perpetual = perpsMarket(marketId = "btc-perp")

        val result =
            MarketPageMapper.watchlist(
                favorites = listOf(crypto, stock),
                stockCoinIds = setOf("hood"),
                perpetualById = mapOf(perpetual.marketId to perpetual),
                subTab = MarketSubTab.ALL,
            )

        assertTrue(result[0] is MarketListEntry.Perpetual)
        assertEquals("spot:btc", result[1].stableId)
        assertEquals(SpotMarketType.STOCK, (result[2] as MarketListEntry.Spot).type)
    }

    @Test
    fun watchlistPerpetualContainsOnlyLinkedFavorites() {
        val linked = market(coinId = "btc", perpsMarketId = "btc-perp", favored = true)
        val unlinked = market(coinId = "eth", favored = true)

        val result =
            MarketPageMapper.watchlist(
                favorites = listOf(linked, unlinked),
                stockCoinIds = emptySet(),
                perpetualById = mapOf("btc-perp" to perpsMarket("btc-perp")),
                subTab = MarketSubTab.PERPETUAL,
            )

        assertEquals(listOf("perpetual:btc-perp"), result.map { it.stableId })
    }

    @Test
    fun perpetualSevenDayChangeStaysUnavailableUntilApiContractExists() {
        val market = perpsMarket(marketId = "btc-perp", change = "0.12")
        val entry = MarketListEntry.Perpetual(market, null)

        assertNull(entry.changePercent(MarketPriceChangePeriod.SEVEN_DAYS))
        assertEquals("12.00", entry.changePercent(MarketPriceChangePeriod.TWENTY_FOUR_HOURS)?.toPlainString())
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
