package one.mixin.android.ui.home.web3.trade

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TradeInputTest {
    @Test
    fun tradeAmountInputUsesWalletSpecificDecimalPlaces() {
        assertEquals(8, tradeInputMaxDecimalPlaces(isCommonWallet = false, precision = 18))
        assertEquals(6, tradeInputMaxDecimalPlaces(isCommonWallet = true, precision = 6))
        assertEquals(0, tradeInputMaxDecimalPlaces(isCommonWallet = true, precision = 0))
        assertEquals(8, tradeInputMaxDecimalPlaces(isCommonWallet = true, precision = -1))
    }

    @Test
    fun tradePriceInputAllowsAtMostEightDecimalPlaces() {
        assertEquals(8, tradePriceInputMaxDecimalPlaces())
        assertTrue(isTradeInputDecimalAllowed("1.12345678", tradePriceInputMaxDecimalPlaces()))
        assertFalse(isTradeInputDecimalAllowed("1.123456789", tradePriceInputMaxDecimalPlaces()))
    }

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

    @Test
    fun swapInputPrecisionChangePreservesCurrentInput() {
        assertEquals(
            "12.123456",
            swapInputTextForMaxDecimalPlacesChange("12.123456789", maxDecimalPlaces = 6)
        )
    }
}
