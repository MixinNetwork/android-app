package one.mixin.android.db.perps

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.api.response.perps.PerpsFavorite
import one.mixin.android.db.PerpsDatabase
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PerpsMarketDaoTest {
    private lateinit var database: PerpsDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room.inMemoryDatabaseBuilder(context, PerpsDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun allMarketsRemainOrderedByVolume() =
        runBlocking {
            database.perpsMarketDao().upsertList(
                listOf(
                    market("highest-volume", volume = "1000", score = 10),
                    market("highest-score", volume = "1", score = 30),
                    market("middle-score", volume = "10", score = 20),
                ),
            )

            val result = database.perpsMarketDao().observeAllMarkets().first()

            assertEquals(
                listOf("highest-volume", "middle-score", "highest-score"),
                result.map(PerpsMarket::marketId),
            )
        }

    @Test
    fun nonTrendingRefreshPreservesExistingTradeVolumeScore() =
        runBlocking {
            val cached = market("cached", volume = "10", score = 30)
            database.perpsMarketDao().upsertSuspend(cached)

            val merged =
                database.perpsMarketDao().upsertPreservingTradeVolumeScores(
                    listOf(
                        cached.copy(last = "2", tradeVolumeScore1D = 0),
                        market("uncached", volume = "20", score = 50),
                    ),
                )

            assertEquals(30, merged.first { it.marketId == "cached" }.tradeVolumeScore1D)
            assertEquals("2", database.perpsMarketDao().getMarket("cached")?.last)
            assertEquals(30, database.perpsMarketDao().getMarket("cached")?.tradeVolumeScore1D)
            assertEquals(0, database.perpsMarketDao().getMarket("uncached")?.tradeVolumeScore1D)
        }

    @Test
    fun searchMarketsMatchesDisplayTokenAndQuoteSymbols() =
        runBlocking {
            database.perpsMarketDao().upsertList(
                listOf(
                    market("btc-usdt", volume = "100", score = 0).copy(
                        displaySymbol = "BTCUSDT",
                        tokenSymbol = "BTC",
                        quoteSymbol = "USDT",
                    ),
                    market("eth-btc", volume = "50", score = 0).copy(
                        displaySymbol = "ETHBTC",
                        tokenSymbol = "ETH",
                        quoteSymbol = "BTC",
                    ),
                    market("zero-volume", volume = "0", score = 0).copy(
                        displaySymbol = "BTCUSD",
                    ),
                ),
            )

            val result = database.perpsMarketDao().searchMarkets("btc")

            assertEquals(listOf("btc-usdt", "eth-btc"), result.map(PerpsMarket::marketId))
        }

    @Test
    fun favoriteMarketsOrderByNewestAddition() =
        runBlocking {
            val markets =
                listOf(
                    market("old", volume = "100", score = 0),
                    market("same-first", volume = "1", score = 0),
                    market("same-second", volume = "10", score = 0),
                    market("new", volume = "2", score = 0),
                )
            database.perpsMarketDao().upsertList(markets)
            database.perpsFavoriteDao().insertSuspend(
                PerpsFavorite("old", true, "2026-08-26T00:00:00Z"),
                PerpsFavorite("same-first", true, "2026-08-26T01:00:00Z"),
                PerpsFavorite("same-second", true, "2026-08-26T01:00:00Z"),
                PerpsFavorite("new", true, "2026-08-26T02:00:00Z"),
            )

            val result = database.perpsFavoriteDao().observeFavoriteMarkets().first()
            val favoriteMarketIds = database.perpsFavoriteDao().observeFavoriteMarketIds().first()

            assertEquals(listOf("new", "same-first", "same-second", "old"), result.map { it.marketId })
            assertEquals(listOf("new", "same-first", "same-second", "old"), favoriteMarketIds)
        }

    private fun market(
        marketId: String,
        volume: String,
        score: Int,
    ) = PerpsMarket(
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
