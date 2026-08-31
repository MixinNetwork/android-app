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
    fun nativeSolSwapBalanceCheckAllowsFullTransferOrLeavesRent() {
        val balance = BigDecimal("1")

        assertFalse(isNativeSolSwapBalanceError("1", balance, isNativeSol = true))
        assertFalse(
            isNativeSolSwapBalanceError(
                balance.subtract(SOLANA_RENT_EXEMPTION).toPlainString(),
                balance,
                isNativeSol = true,
            ),
        )
        assertTrue(
            isNativeSolSwapBalanceError(
                balance.subtract(SOLANA_RENT_EXEMPTION).add(BigDecimal("0.00000001")).toPlainString(),
                balance,
                isNativeSol = true,
            ),
        )
        assertTrue(isNativeSolSwapBalanceError("0.9995", balance, isNativeSol = true))
        assertFalse(isNativeSolSwapBalanceError("0.00000001", balance, isNativeSol = true))
        assertFalse(isNativeSolSwapBalanceError("0.9995", balance, isNativeSol = false))
    }
}
