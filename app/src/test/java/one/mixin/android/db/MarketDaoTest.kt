package one.mixin.android.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import one.mixin.android.vo.market.Market
import one.mixin.android.vo.market.MarketCapRank
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
