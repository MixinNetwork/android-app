package one.mixin.android.ui.home.web3.trade.perps

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PerpsActivityRoutingTest {
    @Test
    fun existingPositionKeepsMarketDetail() {
        assertFalse(canOpenNewPerpsPosition(hasOpenPosition = true))
    }

    @Test
    fun missingPositionOpensNewPositionPage() {
        assertTrue(canOpenNewPerpsPosition(hasOpenPosition = false))
    }
}
