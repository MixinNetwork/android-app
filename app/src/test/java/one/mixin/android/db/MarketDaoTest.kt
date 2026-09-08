package one.mixin.android.db

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import one.mixin.android.vo.market.Market
import one.mixin.android.vo.market.MarketCapRank
import one.mixin.android.vo.market.MarketCategoryRelation
import one.mixin.android.vo.market.MarketFavored
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MarketDaoTest {
    private lateinit var database: MixinDatabase

    @BeforeTest
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room.inMemoryDatabaseBuilder(context, MixinDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun unrankedMarketsAreExcluded() =
        runBlocking {
            database.marketDao().upsertSuspend(market("btc"))

            val result = database.marketDao().observeAllMarkets().first()

            assertEquals(emptyList(), result)
        }

    @Test
    fun rankedMarketsAreIncluded() =
        runBlocking {
            database.marketDao().upsertSuspend(market("btc"))
            database.marketCapRankDao().insertSuspend(MarketCapRank("btc", "1", ""))

            val result = database.marketDao().observeAllMarkets().first()

            assertEquals(listOf("btc"), result.map { it.coinId })
        }

    @Test
    fun rankedMarketsAreSortedByNumericRank() =
        runBlocking {
            database.marketDao().upsertSuspend(market("rank-10", marketCapRank = "1"))
            database.marketDao().upsertSuspend(market("rank-2", marketCapRank = "20"))
            database.marketCapRankDao().insertSuspend(
                MarketCapRank("rank-10", "10", ""),
                MarketCapRank("rank-2", "2", ""),
            )

            val result = database.marketDao().observeAllMarkets().first()

            assertEquals(listOf("rank-2", "rank-10"), result.map { it.coinId })
        }

    @Test
    fun favoriteStateIsProjected() =
        runBlocking {
            database.marketDao().upsertSuspend(market("btc"))
            database.marketCapRankDao().insertSuspend(MarketCapRank("btc", "1", ""))
            database.marketFavoredDao().insertSuspend(MarketFavored("btc", true, ""))

            val result = database.marketDao().observeAllMarkets().first()

            assertEquals(true, result.single().isFavored)
        }

    @Test
    fun favoriteMarketsOrderByNewestAddition() =
        runBlocking {
            val markets = listOf(market("old"), market("same-first"), market("same-second"), market("new"))
            database.marketDao().upsertList(markets)
            database.marketFavoredDao().insertSuspend(
                MarketFavored("old", true, "2026-08-26T00:00:00Z"),
                MarketFavored("same-first", true, "2026-08-26T01:00:00Z"),
                MarketFavored("same-second", true, "2026-08-26T01:00:00Z"),
                MarketFavored("new", true, "2026-08-26T02:00:00Z"),
            )

            val result = database.marketDao().observeFavoredMarkets().first()

            assertEquals(listOf("new", "same-first", "same-second", "old"), result.map { it.coinId })
        }

    @Test
    fun favoriteMarketIdsOnlyReturnsActiveFavorites() =
        runBlocking {
            database.marketFavoredDao().insertSuspend(
                MarketFavored("btc", true, ""),
                MarketFavored("eth", false, ""),
            )

            assertEquals(listOf("btc"), database.marketFavoredDao().favoriteMarketIds())
        }

    @Test
    fun allMarketsAreLimitedToRankedPage() =
        runBlocking {
            val markets = (1..501).map { rank -> market("coin-$rank", rank.toString()) }
            database.marketDao().upsertList(markets)
            database.marketCapRankDao().insertListSuspend(
                markets.map { market ->
                    MarketCapRank(
                        coinId = market.coinId,
                        marketCapRank = market.marketCapRank,
                        updatedAt = "",
                    )
                },
            )

            val result = database.marketDao().observeAllMarkets().first()

            assertEquals(500, result.size)
            assertEquals("coin-1", result.first().coinId)
            assertEquals("coin-500", result.last().coinId)
        }

    @Test
    fun categoryMarketsPreserveApiOrder() =
        runBlocking {
            val markets =
                listOf(
                    market("first", marketCapRank = "100"),
                    market("second", marketCapRank = "1"),
                    market("third", marketCapRank = "10"),
                )
            database.marketDao().upsertList(markets)
            database.marketCategoryDao().replaceCategory(
                category = 1,
                coinIds = markets.map(Market::coinId),
            )

            val result = database.marketCategoryDao().observeMarketsByCategory(1).first()

            assertEquals(markets.map(Market::coinId), result.map { it.coinId })
        }

    @Test
    fun deletesMarketsWithoutCategoryRankOrFavorite() =
        runBlocking {
            database.marketDao().upsertList(
                listOf(
                    market("valid"),
                    market("without-category"),
                    market("without-rank"),
                    market("without-category-or-rank"),
                    market("without-category-or-rank-but-favored"),
                    market("without-category-or-rank-but-unfavored"),
                ),
            )
            database.marketCategoryDao().insertSuspend(MarketCategoryRelation("valid", 1))
            database.marketCategoryDao().insertSuspend(MarketCategoryRelation("without-rank", 1))
            database.marketCapRankDao().insertListSuspend(
                listOf(
                    MarketCapRank("valid", "1", ""),
                    MarketCapRank("without-category", "2", ""),
                ),
            )
            database.marketFavoredDao().insertSuspend(
                MarketFavored("without-category-or-rank-but-favored", true, ""),
                MarketFavored("without-category-or-rank-but-unfavored", false, ""),
            )

            assertEquals(2, database.marketDao().deleteMarketsWithoutCategoryRankOrFavorite())
            assertEquals("valid", database.marketDao().findMarketById("valid")?.coinId)
            assertEquals("without-category", database.marketDao().findMarketById("without-category")?.coinId)
            assertEquals("without-rank", database.marketDao().findMarketById("without-rank")?.coinId)
            assertEquals(null, database.marketDao().findMarketById("without-category-or-rank"))
            assertEquals(
                "without-category-or-rank-but-favored",
                database.marketDao().findMarketById("without-category-or-rank-but-favored")?.coinId,
            )
            assertEquals(null, database.marketDao().findMarketById("without-category-or-rank-but-unfavored"))
        }

    private fun market(
        coinId: String,
        marketCapRank: String = "1",
    ) =
        Market(
            coinId = coinId,
            name = "Bitcoin",
            symbol = "BTC",
            iconUrl = "",
            currentPrice = "1",
            marketCap = "1",
            marketCapRank = marketCapRank,
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
        )
}
