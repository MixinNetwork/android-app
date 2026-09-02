package one.mixin.android.ui.home.web3.market

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import one.mixin.android.vo.market.Market
import one.mixin.android.vo.market.MarketItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketSearchViewModelSearchTest {
    @Test
    fun spotSearch_callsRemoteEvenWhenLocalMatchesAreNonEmpty() =
        runBlocking {
            val local = listOf(market("btc-local"))
            var remoteCalls = 0

            searchSpotMarketsOnlineFirst(
                query = "btc",
                searchLocalMarkets = { local },
                refreshOnlineMarkets = { remoteCalls++ },
                resolveMarketItem = { MarketItem.fromMarket(it) },
            )

            assertEquals(1, remoteCalls)
        }

    @Test
    fun spotSearch_appendsRemoteOnlyMatchesAfterRefresh() =
        runBlocking {
            val initialLocal = listOf(market("btc-usdt"))
            val refreshedLocal = listOf(market("btc-usdt"), market("btc-usdc"))
            var searchCalls = 0

            val result =
                searchSpotMarketsOnlineFirst(
                    query = "btc",
                    searchLocalMarkets = {
                        searchCalls += 1
                        if (searchCalls == 1) initialLocal else refreshedLocal
                    },
                    refreshOnlineMarkets = {},
                    resolveMarketItem = { MarketItem.fromMarket(it) },
                )

            assertEquals(listOf("btc-usdt", "btc-usdc"), result.map(MarketItem::coinId))
        }

    @Test
    fun spotSearch_preservesLocalOrderButUsesRefreshedOverlappingMatches() =
        runBlocking {
            val staleLocal = listOf(market("btc").copy(name = "Local BTC"), market("eth").copy(name = "Local ETH"))
            val refreshedLocal =
                listOf(
                    market("eth").copy(name = "Remote ETH"),
                    market("btc").copy(name = "Remote BTC"),
                    market("btc-cash"),
                )
            var searchCalls = 0

            val result =
                searchSpotMarketsOnlineFirst(
                    query = "btc",
                    searchLocalMarkets = {
                        searchCalls += 1
                        if (searchCalls == 1) staleLocal else refreshedLocal
                    },
                    refreshOnlineMarkets = {},
                    resolveMarketItem = { MarketItem.fromMarket(it) },
                )

            assertEquals(listOf("btc", "eth", "btc-cash"), result.map(MarketItem::coinId))
            assertEquals("Remote BTC", result.first().name)
            assertEquals("Remote ETH", result[1].name)
        }

    @Test
    fun spotSearch_returnsLocalMatchesWhenRemoteRefreshFails() =
        runBlocking {
            val local = listOf(market("btc"))

            val result =
                searchSpotMarketsOnlineFirst(
                    query = "btc",
                    searchLocalMarkets = { local },
                    refreshOnlineMarkets = { error("offline") },
                    resolveMarketItem = { MarketItem.fromMarket(it) },
                )

            assertEquals(listOf("btc"), result.map(MarketItem::coinId))
        }

    @Test(expected = CancellationException::class)
    fun spotSearch_propagatesCancellation() {
        runBlocking {
            searchSpotMarketsOnlineFirst(
                query = "btc",
                searchLocalMarkets = { listOf(market("btc")) },
                refreshOnlineMarkets = { throw CancellationException("cancelled") },
                resolveMarketItem = { MarketItem.fromMarket(it) },
            )
        }
    }

    @Test
    fun spotSearch_blankQuerySkipsLocalAndRemoteSearch() =
        runBlocking {
            var localCalls = 0
            var remoteCalls = 0

            val result =
                searchSpotMarketsOnlineFirst(
                    query = "   ",
                    searchLocalMarkets = {
                        localCalls += 1
                        emptyList()
                    },
                    refreshOnlineMarkets = { remoteCalls += 1 },
                    resolveMarketItem = { MarketItem.fromMarket(it) },
                )

            assertTrue(result.isEmpty())
            assertEquals(0, localCalls)
            assertEquals(0, remoteCalls)
        }

    private fun market(coinId: String) =
        Market(
            coinId = coinId,
            name = coinId.uppercase(),
            symbol = coinId.uppercase(),
            iconUrl = "",
            currentPrice = "1",
            marketCap = "1",
            marketCapRank = "1",
            totalVolume = "1",
            high24h = "1",
            low24h = "1",
            priceChange24h = "0",
            priceChangePercentage1H = "0",
            priceChangePercentage24H = "0",
            priceChangePercentage7D = "0",
            priceChangePercentage30D = "0",
            marketCapChange24h = "0",
            marketCapChangePercentage24h = "0",
            circulatingSupply = "1",
            totalSupply = "1",
            maxSupply = "1",
            ath = "1",
            athChangePercentage = "0",
            athDate = "",
            atl = "1",
            atlChangePercentage = "0",
            atlDate = "",
            assetIds = emptyList(),
            sparklineIn7d = "",
            sparklineIn24h = "",
            updatedAt = "",
            descriptions = null,
            perpsMarketId = null,
        )
}
