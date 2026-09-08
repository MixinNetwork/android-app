package one.mixin.android.ui.home.web3.market

import one.mixin.android.util.analytics.AnalyticsTracker
import org.junit.Assert.assertEquals
import org.junit.Test

class MarketAnalyticsTest {
    @Test
    fun stockPrimaryTabUsesStockAnalyticsValue() {
        assertEquals(AnalyticsTracker.MarketsTab.STOCK, MarketTopTab.STOCK.analyticsValue())
    }

    @Test
    fun watchlistSecondaryTabsUseDistinctAnalyticsValues() {
        assertEquals(AnalyticsTracker.MarketsTab.CRYPTO, MarketSubTab.CRYPTO.analyticsValue())
        assertEquals(AnalyticsTracker.MarketsTab.PERPETUAL, MarketSubTab.PERPETUAL.analyticsValue())
    }
}
