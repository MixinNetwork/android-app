package one.mixin.android.ui.home.web3.market

import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.vo.RecentSearch
import one.mixin.android.vo.RecentSearchType
import org.junit.Assert.assertEquals
import org.junit.Test

class MarketSearchModelsTest {
    @Test
    fun emptyQueryShowsOnlySpotAndPerpetualTabs() {
        assertEquals(
            listOf(MarketSearchTab.CRYPTO, MarketSearchTab.PERPETUAL),
            marketSearchTabs(""),
        )
    }

    @Test
    fun keywordShowsAllTabs() {
        assertEquals(MarketSearchTab.entries, marketSearchTabs("btc"))
    }

    @Test
    fun enteringKeywordStartsOnAllTab() {
        assertEquals(
            MarketSearchTab.ALL,
            selectedMarketSearchTab("", "btc", MarketSearchTab.CRYPTO),
        )
        assertEquals(
            MarketSearchTab.CRYPTO,
            selectedMarketSearchTab("btc", "", MarketSearchTab.PERPETUAL),
        )
    }

    @Test
    fun perpetualTrendingUsesScoreBeforeVolume() {
        val markets =
            listOf(
                market("high-volume", volume = "1000", score = 1),
                market("high-score", volume = "1", score = 3),
                market("middle-score", volume = "10", score = 2),
            )

        assertEquals(
            listOf("high-score", "middle-score", "high-volume"),
            markets.sortedForTrendingSearch().map(PerpsMarket::marketId),
        )
    }

    @Test
    fun marketSearchRanksExactFieldsThenVolumeAndAlphabetically() {
        val markets =
            listOf(
                market("volume", volume = "100", tokenSymbol = "BTC-X"),
                market("name", volume = "1", tokenSymbol = "AAA", displaySymbol = "BTC"),
                market("symbol", volume = "0", tokenSymbol = "BTC"),
                market("alpha-b", volume = "10", tokenSymbol = "ETH"),
                market("alpha-a", volume = "10", tokenSymbol = "AAA"),
            )

        assertEquals(
            listOf("symbol", "name", "volume", "alpha-a", "alpha-b"),
            markets
                .sortedForMarketSearch(
                    query = "btc",
                    symbol = PerpsMarket::tokenSymbol,
                    name = PerpsMarket::displaySymbol,
                    volume = PerpsMarket::volume,
                )
                .map(PerpsMarket::marketId),
        )
    }

    @Test
    fun recentSearchesKeepOnlySpotAndPerpetualMarkets() {
        val searches =
            listOf(
                RecentSearch(RecentSearchType.ASSET, title = "USDT", primaryKey = "eth-usdt"),
                RecentSearch(RecentSearchType.ASSET, title = "BTC", primaryKey = "btc"),
                RecentSearch(RecentSearchType.ASSET, title = "usdt", primaryKey = "tron-usdt"),
                RecentSearch(RecentSearchType.MARKET, title = "ETH", primaryKey = "eth"),
                RecentSearch(RecentSearchType.PERPETUAL, title = "BTCUSDT", primaryKey = "btc-perp"),
            )

        assertEquals(
            listOf("ETH", "BTCUSDT"),
            searches.marketRecentSearches().map { it.title },
        )
    }

    @Test
    fun addingRecentSearchMovesDuplicateToFrontAndKeepsMarketTypesSeparate() {
        val searches =
            listOf(
                RecentSearch(RecentSearchType.MARKET, title = "BTC", primaryKey = "btc"),
                RecentSearch(RecentSearchType.PERPETUAL, title = "BTCUSDT", primaryKey = "btc"),
            )

        val updated =
            searches.addMarketRecentSearch(
                RecentSearch(RecentSearchType.MARKET, title = "BTC", primaryKey = "btc"),
            )

        assertEquals(
            listOf(RecentSearchType.MARKET, RecentSearchType.PERPETUAL),
            updated.map(RecentSearch::type),
        )
        assertEquals("BTC", updated.first().title)
        assertEquals("BTCUSDT", updated.last().title)
    }

    private fun market(
        marketId: String,
        volume: String,
        score: Int = 0,
        displaySymbol: String = marketId,
        tokenSymbol: String = marketId,
    ) = PerpsMarket(
        marketId = marketId,
        displaySymbol = displaySymbol,
        tokenSymbol = tokenSymbol,
        quoteSymbol = "USD",
        markPrice = "1",
        leverage = 10,
        iconUrl = "",
        fundingRate = "0",
        minAmount = "0",
        maxAmount = "0",
        last = "1",
        volume = volume,
        tradeVolumeScore1D = score,
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
