package one.mixin.android.tip.wc.internal

import org.junit.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WcBitcoinFeeEstimateTest {
    @Test
    fun capsServerFeeRate() {
        assertEquals(0, BigDecimal("10").compareTo(WcBitcoinFeeEstimate.cappedFeeRate(BigDecimal.TEN)))
        assertEquals(0, BigDecimal("100").compareTo(WcBitcoinFeeEstimate.cappedFeeRate(BigDecimal("1000"))))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonPositiveRate() {
        WcBitcoinFeeEstimate.cappedFeeRate(BigDecimal.ZERO)
    }

    @Test
    fun rejectsIncreasedOrInvalidFinalFee() {
        val confirmed = WcBitcoinFeeEstimate(BigDecimal.TEN, BigDecimal("0.00001"), null)
        assertTrue(confirmed.permitsFee(BigDecimal("0.00001000")))
        assertTrue(confirmed.permitsFee(BigDecimal("0.00000999")))
        assertFalse(confirmed.permitsFee(BigDecimal("0.00001001")))
        assertFalse(confirmed.permitsFee(BigDecimal.ZERO))
        assertFalse(confirmed.permitsFee(BigDecimal("-1")))
        assertFalse(confirmed.copy(feeRate = BigDecimal("101")).permitsFee(confirmed.feeBtc))
        assertFalse(confirmed.copy(feeRate = BigDecimal.ZERO).permitsFee(confirmed.feeBtc))
        assertFalse(confirmed.copy(feeBtc = BigDecimal.ZERO).permitsFee(confirmed.feeBtc))
    }

    @Test
    fun rejectsFeeBelowServerMinimum() {
        val confirmed = WcBitcoinFeeEstimate(BigDecimal.TEN, BigDecimal("0.00001"), BigDecimal("0.000005"))
        assertFalse(confirmed.permitsFee(BigDecimal("0.000004")))
        assertTrue(confirmed.permitsFee(BigDecimal("0.000005")))
    }
}
