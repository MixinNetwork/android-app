package one.mixin.android.ui.wallet.home

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class WalletHomeBalanceTest {
    @Test
    fun totalFiatAddsTokenAndPositionFiat() {
        val total = calculateWalletHomeTotalFiat(
            tokenFiat = BigDecimal("100.25"),
            positionFiat = BigDecimal("25.25"),
        )

        assertEquals(0, total.compareTo(BigDecimal("125.50")))
    }

    @Test
    fun positionMarginFiatTotalIgnoresInvalidMarginsAndAppliesRate() {
        val total = calculateWalletHomePositionMarginFiat(
            margins = listOf("10", null, "bad", "-2.5"),
            fiatRate = BigDecimal("7.2"),
        )

        assertEquals(0, total.compareTo(BigDecimal("54.00")))
    }
}
