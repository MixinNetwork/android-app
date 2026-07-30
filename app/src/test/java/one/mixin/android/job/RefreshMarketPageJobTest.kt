package one.mixin.android.job

import one.mixin.android.event.MarketPageDataSource
import one.mixin.android.vo.market.MarketCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class RefreshMarketPageJobTest {
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
}
