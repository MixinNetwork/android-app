package one.mixin.android.db.perps

import android.content.Context
import androidx.room3.Room
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
    fun minimumOrderValueSurvivesApiParsingAndDatabaseStorage() = runBlocking {
        val gson = com.google.gson.Gson()
        val json = gson.toJsonTree(market("minimum", volume = "1", score = 0)).asJsonObject
        json.addProperty("min_order_value", "10.25")
        val parsed = gson.fromJson(json, PerpsMarket::class.java)
        database.perpsMarketDao().upsertSuspend(parsed)
        assertEquals("10.25", database.perpsMarketDao().getMarket("minimum")?.minOrderValue)
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
    fun searchMarketsMatchesTagsAndPreservesVolumeFilteringAndOrder() =
        runBlocking {
            val tags = listOf("sandisk", "\u95ea\u8fea")
            database.perpsMarketDao().upsertList(
                listOf(
                    market("lower-volume", volume = "9", score = 0).copy(tags = tags),
                    market("higher-volume", volume = "100", score = 0).copy(tags = tags),
                    market("zero-volume", volume = "0", score = 0).copy(tags = tags),
                    market("no-tags", volume = "200", score = 0),
                ),
            )

            for (query in listOf("\u95ea\u8fea", "\u95ea", "SanDisk", "disk")) {
                assertEquals(
                    listOf("higher-volume", "lower-volume"),
                    database.perpsMarketDao().searchMarkets(query).map(PerpsMarket::marketId),
                )
            }
        }

    @Test
    fun searchMarketsIgnoresSerializedTagJsonSyntax() =
        runBlocking {
            database.perpsMarketDao().upsertList(
                listOf(
                    market("untagged", volume = "300", score = 0),
                    market("normal", volume = "200", score = 0).copy(tags = listOf("alpha", "beta")),
                    market("escaped", volume = "100", score = 0).copy(tags = listOf("AT&T", "a<b>c")),
                ),
            )

            for (query in listOf("[", "]", "\"", ",", "alpha\",\"beta", "alphabeta", "alpha beta", "alpha,beta", "u0026", "u003c")) {
                assertEquals(
                    "query: $query",
                    emptyList<String>(),
                    database.perpsMarketDao().searchMarkets(query).map(PerpsMarket::marketId),
                )
            }
        }

    @Test
    fun searchMarketsMatchesTagsAsLiteralText() =
        runBlocking {
            val tags = listOf("AT&T", "a<b>c", "a=b", "it's", "say \"hi\"", "a\\b", "[x]", "a,b", "100%", "c_d")
            database.perpsMarketDao().upsertList(
                tags.mapIndexed { index, tag ->
                    market("tag-$index", volume = "100", score = 0).copy(tags = listOf(tag))
                } + market("plain", volume = "200", score = 0).copy(tags = listOf("100x", "cxd")),
            )

            val queries = mapOf(
                "AT&T" to 0, "&" to 0, "<" to 1, ">" to 1, "=" to 2, "'" to 3,
                "\"hi\"" to 4, "\\" to 5, "[x]" to 6, "a,b" to 7, "%" to 8, "_" to 9, "c_d" to 9,
            )
            for ((query, index) in queries) {
                assertEquals(
                    "query: $query",
                    listOf("tag-$index"),
                    database.perpsMarketDao().searchMarkets(query).map(PerpsMarket::marketId),
                )
            }
        }

    @Test
    fun upsertedRefreshedMarketsBecomeSearchableWithoutClearingDatabase() =
        runBlocking {
            database.perpsMarketDao().upsertList(
                listOf(
                    market("eth-usdt", volume = "50", score = 0).copy(
                        displaySymbol = "ETHUSDT",
                        tokenSymbol = "ETH",
                        quoteSymbol = "USDT",
                    ),
                ),
            )

            database.perpsMarketDao().upsertList(
                listOf(
                    market("btc-usdt", volume = "100", score = 0).copy(
                        displaySymbol = "BTCUSDT",
                        tokenSymbol = "BTC",
                        quoteSymbol = "USDT",
                    ),
                    market("eth-usdt", volume = "60", score = 0).copy(
                        displaySymbol = "ETHUSDT",
                        tokenSymbol = "ETH",
                        quoteSymbol = "USDT",
                    ),
                ),
            )

            val result = database.perpsMarketDao().searchMarkets("btc")

            assertEquals(listOf("btc-usdt"), result.map(PerpsMarket::marketId))
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
