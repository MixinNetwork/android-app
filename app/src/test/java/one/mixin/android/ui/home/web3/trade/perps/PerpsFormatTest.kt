package one.mixin.android.ui.home.web3.trade.perps

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class PerpsFormatTest {
    @Test
    fun exactUsdUsesGroupingAndAtLeastTwoFractionDigits() {
        assertEquals(
            "$1,234.50",
            formatPerpsExactUsdDecimal(BigDecimal("1234.5")),
        )
        assertEquals(
            "$0.80",
            formatPerpsExactUsdDecimal(BigDecimal("0.8")),
        )
    }

    @Test
    fun exactUsdCapsServerPrecision() {
        assertEquals(
            "$1.12345679",
            formatPerpsExactUsdDecimal(BigDecimal("1.123456789")),
        )
    }

    @Test
    fun feeShowsFullAmountInsteadOfLessThanOneCent() {
        assertEquals(
            "-$0.003421",
            formatPerpsSignedExactUsdDecimal(BigDecimal("-0.003421")),
        )
    }

    @Test
    fun tinyPositiveFeeKeepsFullDigits() {
        assertEquals(
            "+$0.0008",
            formatPerpsSignedExactUsdDecimal(BigDecimal("0.000800")),
        )
    }

    @Test
    fun rawUsdStillAbbreviatesTinyPnl() {
        assertEquals(
            "<$0.01",
            formatPerpsRawUsdDecimal(BigDecimal("0.003421")),
        )
    }
}
