package one.mixin.android.vo.market

import one.mixin.android.util.ErrorHandler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketRefreshTest {
    @Test
    fun topMoversUseSharedPageLimit() {
        assertEquals(100, marketRefreshLimit(MarketCategory.TOP_GAINER))
        assertEquals(100, marketRefreshLimit(MarketCategory.TOP_LOSER))
    }

    @Test
    fun stockAndTrendingKeepFullRefreshLimit() {
        assertEquals(500, marketRefreshLimit(MarketCategory.STOCK))
        assertEquals(500, marketRefreshLimit(MarketCategory.TRENDING))
    }

    @Test
    fun oldVersionFailuresAreAggregatedToOneSignal() {
        val results =
            listOf(
                MarketRefreshResult.Failure(ErrorHandler.OLD_VERSION),
                MarketRefreshResult.Failure(ErrorHandler.OLD_VERSION),
                MarketRefreshResult.Failure(500),
            )

        assertTrue(results.hasErrorCode(ErrorHandler.OLD_VERSION))
        assertFalse(results.hasErrorCode(401))
    }
}
