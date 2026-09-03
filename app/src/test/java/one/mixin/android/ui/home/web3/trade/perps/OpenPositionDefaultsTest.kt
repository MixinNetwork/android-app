package one.mixin.android.ui.home.web3.trade.perps

import kotlin.test.Test
import kotlin.test.assertEquals

class OpenPositionDefaultsTest {
    @Test
    fun requestedLeverageOverridesSavedValueWithinMarketBounds() {
        assertEquals(
            20,
            resolveInitialPerpsLeverage(
                requestedLeverage = 20,
                savedLeverage = 5,
                maxLeverage = 100,
            ),
        )
    }

    @Test
    fun requestedLeverageIsBoundedByMarket() {
        assertEquals(
            100,
            resolveInitialPerpsLeverage(
                requestedLeverage = 200,
                savedLeverage = 5,
                maxLeverage = 100,
            ),
        )
        assertEquals(
            1,
            resolveInitialPerpsLeverage(
                requestedLeverage = 0,
                savedLeverage = 5,
                maxLeverage = 100,
            ),
        )
    }

    @Test
    fun savedLeverageIsUsedWhenSchemaOmitsIt() {
        assertEquals(
            10,
            resolveInitialPerpsLeverage(
                requestedLeverage = null,
                savedLeverage = 10,
                maxLeverage = 100,
            ),
        )
    }
}
