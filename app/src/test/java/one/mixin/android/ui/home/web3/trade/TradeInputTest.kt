package one.mixin.android.ui.home.web3.trade

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TradeInputTest {
    @Test
    fun tradeAmountInputAllowsAtMostEightDecimalPlaces() {
        assertTrue(isTradeInputDecimalAllowed(""))
        assertTrue(isTradeInputDecimalAllowed("12"))
        assertTrue(isTradeInputDecimalAllowed("12."))
        assertTrue(isTradeInputDecimalAllowed("12.12345678"))
        assertTrue(isTradeInputDecimalAllowed("0.00000000"))

        assertFalse(isTradeInputDecimalAllowed("12.123456789"))
        assertFalse(isTradeInputDecimalAllowed("0.000000001"))

        assertTrue(isTradeInputDecimalAllowed("12.123456789", maxDecimalPlaces = null))
    }

    @Test
    fun tradeAmountInputLimitsProgrammaticValuesToEightDecimalPlaces() {
        assertEquals("", limitTradeInputDecimalPlaces(""))
        assertEquals("12", limitTradeInputDecimalPlaces("12"))
        assertEquals("12.", limitTradeInputDecimalPlaces("12."))
        assertEquals("12.12345678", limitTradeInputDecimalPlaces("12.12345678"))
        assertEquals("12.12345678", limitTradeInputDecimalPlaces("12.123456789"))
        assertEquals("0.00000000", limitTradeInputDecimalPlaces("0.000000001"))
        assertEquals(
            "12.123456789",
            limitTradeInputDecimalPlaces("12.123456789", maxDecimalPlaces = null)
        )
    }
}
