package one.mixin.android.ui.home.web3.trade.perps

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class PerpsPnlPercentTest {
    @Test
    fun singlePositionUsesServerRoe() {
        val percent = calculateTotalPnlPercent(
            positionCount = 1,
            singlePositionRoe = "1.1074",
            totalPnl = BigDecimal("20.674"),
            totalMargin = BigDecimal("16.69"),
        )

        assertEquals(0, percent.compareTo(BigDecimal("110.74")))
    }

    @Test
    fun multiplePositionsUseAggregatedPnlAndMargin() {
        val percent = calculateTotalPnlPercent(
            positionCount = 2,
            singlePositionRoe = null,
            totalPnl = BigDecimal("30"),
            totalMargin = BigDecimal("20"),
        )

        assertEquals(0, percent.compareTo(BigDecimal("150")))
    }

    @Test
    fun invalidSinglePositionRoeReturnsZero() {
        val percent = calculateTotalPnlPercent(
            positionCount = 1,
            singlePositionRoe = "invalid",
            totalPnl = BigDecimal("30"),
            totalMargin = BigDecimal("20"),
        )

        assertEquals(0, percent.compareTo(BigDecimal.ZERO))
    }

    @Test
    fun nonPositiveMarginWithoutValidSinglePositionRoeReturnsZero() {
        val percent = calculateTotalPnlPercent(
            positionCount = 0,
            singlePositionRoe = null,
            totalPnl = BigDecimal("30"),
            totalMargin = BigDecimal.ZERO,
        )

        assertEquals(0, percent.compareTo(BigDecimal.ZERO))
    }
}
