package one.mixin.android.job

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class RefreshMarketJobTest {
    @Test
    fun marketRefreshDoesNotDeduplicateTokenRefresh() {
        val market = RefreshMarketJob("asset")
        assertNotEquals(RefreshTokensJob("asset").singleInstanceId, market.singleInstanceId)
        assertEquals(RefreshMarketJob("asset").singleInstanceId, market.singleInstanceId)
        assertNotEquals(RefreshMarketJob("another-asset").singleInstanceId, market.singleInstanceId)
    }
}
