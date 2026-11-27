package one.mixin.android.ui.home.web3.trade.perps

import org.junit.Assert.assertEquals
import org.junit.Test

class PerpsProGuideVisibilityTest {
    @Test
    fun activityAlwaysShowsProGuide() {
        assertEquals(true, resolvePerpsProGuideVisibility(hasActivity = true, hasSyncedOrders = false))
        assertEquals(true, resolvePerpsProGuideVisibility(hasActivity = true, hasSyncedOrders = true))
    }

    @Test
    fun noActivityShowsHowPerpsWorksAfterSync() {
        assertEquals(null, resolvePerpsProGuideVisibility(hasActivity = false, hasSyncedOrders = false))
        assertEquals(false, resolvePerpsProGuideVisibility(hasActivity = false, hasSyncedOrders = true))
    }

    @Test
    fun unknownActivityDoesNotShowGuide() {
        assertEquals(null, resolvePerpsProGuideVisibility(hasActivity = null, hasSyncedOrders = false))
        assertEquals(null, resolvePerpsProGuideVisibility(hasActivity = null, hasSyncedOrders = true))
    }
}
