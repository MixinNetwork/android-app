package one.mixin.android.ui.home.web3.trade.perps

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class PerpsFormatTest {
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
