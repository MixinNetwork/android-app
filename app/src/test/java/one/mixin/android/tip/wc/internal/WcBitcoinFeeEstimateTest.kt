package one.mixin.android.tip.wc.internal

import org.junit.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WcBitcoinFeeEstimateTest {
    @Test
    fun capsServerRateAndMinimumFee() {
        assertEquals(BigDecimal("10"), WcBitcoinFeeEstimate.cappedFeeRate(BigDecimal.TEN, null, 100))
        assertEquals(BigDecimal("100"), WcBitcoinFeeEstimate.cappedFeeRate(BigDecimal("1000"), null, 100))
        assertEquals(BigDecimal("100"), WcBitcoinFeeEstimate.cappedFeeRate(BigDecimal.ONE, BigDecimal.ONE, 100))
        assertEquals(0, BigDecimal("20").compareTo(WcBitcoinFeeEstimate.cappedFeeRate(BigDecimal.ONE, BigDecimal("0.00002"), 100)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonPositiveRate() {
        WcBitcoinFeeEstimate.cappedFeeRate(BigDecimal.ZERO, null, 100)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNegativeMinimumFee() {
        WcBitcoinFeeEstimate.cappedFeeRate(BigDecimal.ONE, BigDecimal("-1"), 100)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidVirtualSize() {
        WcBitcoinFeeEstimate.cappedFeeRate(BigDecimal.ONE, null, 0)
    }

    @Test
    fun rejectsIncreasedOrInvalidFinalFee() {
        val confirmed = WcBitcoinFeeEstimate(BigDecimal.TEN, BigDecimal("0.00001"))
        assertTrue(confirmed.permitsFee(BigDecimal("0.00001000")))
        assertTrue(confirmed.permitsFee(BigDecimal("0.00000999")))
        assertFalse(confirmed.permitsFee(BigDecimal("0.00001001")))
        assertFalse(confirmed.permitsFee(BigDecimal.ZERO))
        assertFalse(confirmed.permitsFee(BigDecimal("-1")))
        assertFalse(confirmed.copy(feeRate = BigDecimal("101")).permitsFee(confirmed.feeBtc))
        assertFalse(confirmed.copy(feeRate = BigDecimal.ZERO).permitsFee(confirmed.feeBtc))
        assertFalse(confirmed.copy(feeBtc = BigDecimal.ZERO).permitsFee(confirmed.feeBtc))
    }
}
