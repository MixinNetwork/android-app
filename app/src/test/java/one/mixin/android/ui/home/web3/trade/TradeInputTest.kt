package one.mixin.android.ui.home.web3.trade

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import one.mixin.android.web3.SOLANA_RENT_EXEMPTION

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
    fun inMixinNativeSolSwapUsesFullBalance() {
        assertEquals(
            BigDecimal("1"),
            swapSpendableBalance(
                rawBalance = BigDecimal("1"),
                isNativeSol = true,
                inMixin = true,
            ),
        )
    }

    @Test
    fun selfCustodyNativeSolSwapStillReservesRent() {
        assertEquals(
            BigDecimal("0.99910912"),
            swapSpendableBalance(
                rawBalance = BigDecimal("1"),
                isNativeSol = true,
                inMixin = false,
            ),
        )
    }

    @Test
    fun nonNativeSwapBalanceIsUnchanged() {
        assertEquals(
            BigDecimal("1"),
            swapSpendableBalance(
                rawBalance = BigDecimal("1"),
                isNativeSol = false,
                inMixin = true,
            ),
        )
    }

    @Test
    fun selfCustodyNonNativeSwapBalanceIsUnchanged() {
        assertEquals(
            BigDecimal("1"),
            swapSpendableBalance(
                rawBalance = BigDecimal("1"),
                isNativeSol = false,
                inMixin = false,
            ),
        )
    }

    @Test
    fun inMixinNativeSolSwapRejectsAmountsBelowRent() {
        assertTrue(
            isNativeSolSwapAmountBelowRent(
                inputText = SOLANA_RENT_EXEMPTION.subtract(BigDecimal("0.00000001")).toPlainString(),
                isNativeSol = true,
                inMixin = true,
            ),
        )
        assertFalse(
            isNativeSolSwapAmountBelowRent(
                inputText = SOLANA_RENT_EXEMPTION.toPlainString(),
                isNativeSol = true,
                inMixin = true,
            ),
        )
        assertFalse(
            isNativeSolSwapAmountBelowRent(
                inputText = "0",
                isNativeSol = true,
                inMixin = true,
            ),
        )
        assertFalse(
            isNativeSolSwapAmountBelowRent(
                inputText = "0.00000001",
                isNativeSol = true,
                inMixin = false,
            ),
        )
        assertFalse(
            isNativeSolSwapAmountBelowRent(
                inputText = "0.00000001",
                isNativeSol = false,
                inMixin = true,
            ),
        )
    }
}
