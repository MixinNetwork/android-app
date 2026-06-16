package one.mixin.android.ui.wallet.home

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class WalletHomeBalanceTest {
    @Test
    fun totalFiatAddsTokenFiatAndPositionUsd() {
        val total = calculateWalletHomeTotalFiat(
            tokenFiat = BigDecimal("100.25"),
            positionUsd = BigDecimal("25.25"),
        )

        assertEquals(0, total.compareTo(BigDecimal("125.50")))
    }

    @Test
    fun positionMarginUsdTotalKeepsPositionMarginsInUsd() {
        val total = calculateWalletHomePositionMarginUsd(
            margins = listOf("10", null, "bad", "-2.5"),
        )

        assertEquals(0, total.compareTo(BigDecimal("7.5")))
    }
}
