package one.mixin.android.job

import one.mixin.android.event.ALL_MARKET_PAGE_DATA_SOURCES
import one.mixin.android.event.MarketPageDataSource
import one.mixin.android.vo.market.MarketCategory
import one.mixin.android.vo.market.marketRefreshLimit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshMarketPageJobTest {
    @Test
    fun requiresNetwork() {
        assertTrue(RefreshMarketPageJob("24h").requiresNetwork())
    }

    @Test
    fun refreshesStockCategoryForSpotClassification() {
        val stockRequest =
            RefreshMarketPageJob.SPOT_MARKET_REFRESH_REQUESTS.single {
                it.category == MarketCategory.STOCK.apiValue
            }

        assertEquals(
            MarketPageDataSource.SPOT_ALL,
            stockRequest.source,
        )
    }

    @Test
    fun limitsTopGainerAndLoserRequestsTo100Markets() {
        val requests =
            RefreshMarketPageJob.SPOT_MARKET_REFRESH_REQUESTS.filter {
                it.source == MarketPageDataSource.SPOT_TOP_GAINER ||
                    it.source == MarketPageDataSource.SPOT_TOP_LOSER
            }

        assertEquals(
            listOf(
                marketRefreshLimit(MarketCategory.TOP_GAINER),
                marketRefreshLimit(MarketCategory.TOP_LOSER),
            ),
            requests.map { it.limit },
        )
    }

    @Test
    fun scopedSpotAllRefreshIncludesMarketAndStockRequests() {
        val plan = marketPageRefreshPlan(setOf(MarketPageDataSource.SPOT_ALL))

        assertEquals(
            listOf("all", MarketCategory.STOCK.apiValue),
            plan.spotRequests.map { it.category },
        )
        assertFalse(plan.refreshesPerpetualAll)
        assertFalse(plan.refreshesPerpetualFavorite)
        assertFalse(plan.refreshesPerpetualFeatured)
        assertFalse(plan.refreshesGlobal)
    }

    @Test
    fun fullRefreshIncludesEveryMarketSource() {
        val plan = marketPageRefreshPlan(ALL_MARKET_PAGE_DATA_SOURCES)

        assertEquals(RefreshMarketPageJob.SPOT_MARKET_REFRESH_REQUESTS, plan.spotRequests)
        assertTrue(plan.refreshesPerpetualAll)
        assertTrue(plan.refreshesPerpetualFavorite)
        assertTrue(plan.refreshesPerpetualFeatured)
        assertTrue(plan.refreshesGlobal)
    }
}
