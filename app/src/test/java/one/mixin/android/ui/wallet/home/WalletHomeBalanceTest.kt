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

    @Test
    fun tokenFiatConvertsUsdSummaryWithFiatRate() {
        val total = calculateWalletHomeTokenFiat(
            totalUsd = BigDecimal("100.25"),
            fiatRate = BigDecimal("7.2"),
        )

        assertEquals(0, total.compareTo(BigDecimal("721.800")))
    }

    @Test
    fun btcTotalUsesBitcoinUsdPriceWhenAvailable() {
        val total = calculateWalletHomeBtcTotal(
            tokenFiat = BigDecimal("700"),
            tokenBtc = BigDecimal("0.5"),
            bitcoinPriceUsd = BigDecimal("50000"),
            fiatRate = BigDecimal("7"),
        )

        assertEquals(0, total.compareTo(BigDecimal("0.0020000000000000")))
    }

    @Test
    fun btcTotalFallsBackToTokenBtcWithoutBitcoinUsdPrice() {
        val total = calculateWalletHomeBtcTotal(
            tokenFiat = BigDecimal("700"),
            tokenBtc = BigDecimal("0.5"),
            bitcoinPriceUsd = BigDecimal.ZERO,
            fiatRate = BigDecimal("7"),
        )

        assertEquals(0, total.compareTo(BigDecimal("0.5")))
    }
}
