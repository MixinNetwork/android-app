package one.mixin.android.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import one.mixin.android.api.response.perps.PerpsMarket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PerpsMarketRepositoryTest {
    @Test
    fun perpsSearch_callsSyncEvenWhenLocalMatchesAreNonEmpty() =
        runBlocking {
            val local = listOf(market("btc-usdt"))
            var syncCalls = 0

            searchPerpsMarketsOnlineFirst(
                query = "btc",
                searchLocalMarkets = { local },
                syncOnlineMarkets = { syncCalls += 1 },
            )

            assertEquals(1, syncCalls)
        }

    @Test
    fun perpsSearch_appendsRemoteOnlyMatchesAfterSync() =
        runBlocking {
            val initialLocal = listOf(market("btc-usdt"))
            val refreshedLocal = listOf(market("btc-usdt"), market("btc-usdc"))
            var searchCalls = 0

            val result =
                searchPerpsMarketsOnlineFirst(
                    query = "btc",
                    searchLocalMarkets = {
                        searchCalls += 1
                        if (searchCalls == 1) initialLocal else refreshedLocal
                    },
                    syncOnlineMarkets = {},
                )

            assertEquals(listOf("btc-usdt", "btc-usdc"), result.map(PerpsMarket::marketId))
        }

    @Test
    fun perpsSearch_preservesLocalOrderButUsesRefreshedOverlappingMatchesByMarketId() =
        runBlocking {
            val staleLocal = listOf(market("eth-usdt").copy(last = "1"), market("btc-usdt").copy(last = "1"))
            val refreshedLocal =
                listOf(
                    market("eth-usdt").copy(last = "2"),
                    market("btc-usdt").copy(last = "3"),
                    market("sol-usdt"),
                )
            var searchCalls = 0

            val result =
                searchPerpsMarketsOnlineFirst(
                    query = "usdt",
                    searchLocalMarkets = {
                        searchCalls += 1
                        if (searchCalls == 1) staleLocal else refreshedLocal
                    },
                    syncOnlineMarkets = {},
                )

            assertEquals(listOf("eth-usdt", "btc-usdt", "sol-usdt"), result.map(PerpsMarket::marketId))
            assertEquals("2", result[0].last)
            assertEquals("3", result[1].last)
        }

    @Test
    fun perpsSearch_returnsInitialLocalMatchesWhenSyncFails() =
        runBlocking {
            val local = listOf(market("btc-usdt"))

            val result =
                searchPerpsMarketsOnlineFirst(
                    query = "btc",
                    searchLocalMarkets = { local },
                    syncOnlineMarkets = { error("offline") },
                )

            assertEquals(listOf("btc-usdt"), result.map(PerpsMarket::marketId))
        }

    @Test(expected = CancellationException::class)
    fun perpsSearch_propagatesCancellation() {
        runBlocking {
            searchPerpsMarketsOnlineFirst(
                query = "btc",
                searchLocalMarkets = { listOf(market("btc-usdt")) },
                syncOnlineMarkets = { throw CancellationException("cancelled") },
            )
        }
    }

    @Test
    fun perpsSearch_blankQuerySkipsLocalAndSyncCalls() =
        runBlocking {
            var localCalls = 0
            var syncCalls = 0

            val result =
                searchPerpsMarketsOnlineFirst(
                    query = "   ",
                    searchLocalMarkets = {
                        localCalls += 1
                        emptyList()
                    },
                    syncOnlineMarkets = { syncCalls += 1 },
                )

            assertTrue(result.isEmpty())
            assertEquals(0, localCalls)
            assertEquals(0, syncCalls)
        }

    private fun market(marketId: String) =
        PerpsMarket(
            marketId = marketId,
            displaySymbol = marketId,
            tokenSymbol = marketId,
            quoteSymbol = "USD",
            markPrice = "1",
            leverage = 10,
            iconUrl = "",
            fundingRate = "0",
            minAmount = "0",
            maxAmount = "0",
            last = "1",
            volume = "1",
            tradeVolumeScore1D = 0,
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
