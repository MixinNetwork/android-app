package one.mixin.android.db.perps

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.db.PerpsDatabase
import one.mixin.android.vo.market.MarketCategory
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PerpsMarketCategoryDaoTest {
    private lateinit var database: PerpsDatabase

    @BeforeTest
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room.inMemoryDatabaseBuilder(context, PerpsDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun featuredMarketsPreserveApiOrder() =
        runBlocking {
            val markets =
                listOf(
                    market("first", volume = "1"),
                    market("second", volume = "1000"),
                    market("third", volume = "10"),
                )
            database.perpsMarketDao().upsertList(markets)
            database.perpsMarketCategoryDao().replaceCategory(
                category = MarketCategory.FEATURED.value,
                marketIds = markets.map(PerpsMarket::marketId),
            )

            val result =
                database.perpsMarketCategoryDao()
                    .observeMarketsByCategory(MarketCategory.FEATURED.value)
                    .first()

            assertEquals(markets.map(PerpsMarket::marketId), result.map(PerpsMarket::marketId))
        }

    private fun market(
        marketId: String,
        volume: String,
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
