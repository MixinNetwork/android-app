package one.mixin.android.ui.home.web3.trade.perps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class PerpsTpSlCalculationTest {
    @Test
    fun existingLongUsesActualQuantityAndAddedMargin() {
        val basis = existingBasis(quantity = "10", margin = "200")!!

        assertEquals("110", targetPriceForRoiPercent(basis, "50", isLong = true, isTakeProfit = true, priceScale = 2))
        assertResult(basis.pnlAt(BigDecimal("110"), isLong = true), pnl = "100", roiPercent = "50")
    }

    @Test
    fun existingLongUsesReducedCurrentMargin() {
        val basis = existingBasis(quantity = "10", margin = "50")!!

        assertEquals("102.5", targetPriceForRoiPercent(basis, "50", isLong = true, isTakeProfit = true, priceScale = 2))
        assertResult(basis.pnlAt(BigDecimal("110"), isLong = true), pnl = "100", roiPercent = "200")
    }

    @Test
    fun existingShortUsesAbsoluteQuantityAndDirection() {
        val basis = existingBasis(quantity = "-10", margin = "200")!!

        assertEquals("90", targetPriceForRoiPercent(basis, "50", isLong = false, isTakeProfit = true, priceScale = 2))
        assertResult(basis.pnlAt(BigDecimal("90"), isLong = false), pnl = "100", roiPercent = "50")
        assertEquals("110", targetPriceForRoiPercent(basis, "50", isLong = false, isTakeProfit = false, priceScale = 2))
        assertResult(basis.pnlAt(BigDecimal("110"), isLong = false), pnl = "-100", roiPercent = "-50")
    }

    @Test
    fun existingShortUsesReducedCurrentMargin() {
        val basis = existingBasis(quantity = "10", margin = "50")!!

        assertEquals("97.5", targetPriceForRoiPercent(basis, "50", isLong = false, isTakeProfit = true, priceScale = 2))
        assertResult(basis.pnlAt(BigDecimal("90"), isLong = false), pnl = "100", roiPercent = "200")
    }

    @Test
    fun existingLongStopLossKeepsSignedLoss() {
        val basis = existingBasis(quantity = "10", margin = "200")!!

        assertEquals("90", targetPriceForRoiPercent(basis, "50", isLong = true, isTakeProfit = false, priceScale = 2))
        assertResult(basis.pnlAt(BigDecimal("90"), isLong = true), pnl = "-100", roiPercent = "-50")
    }

    @Test
    fun preOpenEstimationPreservesLeverageConversions() {
        val basis = resolveTpSlCalculationBasis(
            entryPrice = "100",
            currentPrice = "100",
            marginAmount = "100",
            positionQuantity = null,
            leverage = 10,
        )!!

        assertDecimalEquals("10", basis.absoluteQuantity)
        assertEquals("105", targetPriceForRoiPercent(basis, "50", isLong = true, isTakeProfit = true, priceScale = 2))
        assertResult(basis.pnlAt(BigDecimal("105"), isLong = true), pnl = "50", roiPercent = "50")
        assertResult(basis.pnlAt(BigDecimal("95"), isLong = false), pnl = "50", roiPercent = "50")
    }

    @Test
    fun preOpenWithoutMarginAllowsConversionsWithoutPnlPreview() {
        for (margin in listOf("", "0", "0.00")) {
            val basis = resolveTpSlConversionBasis("", "100", margin, null, 10)!!

            assertEquals("105", targetPriceForRoiPercent(basis, "50", isLong = true, isTakeProfit = true, priceScale = 2))
            assertEquals("95", targetPriceForRoiPercent(basis, "50", isLong = true, isTakeProfit = false, priceScale = 2))
            assertEquals("95", targetPriceForRoiPercent(basis, "50", isLong = false, isTakeProfit = true, priceScale = 2))
            assertEquals("105", targetPriceForRoiPercent(basis, "50", isLong = false, isTakeProfit = false, priceScale = 2))
            assertDecimalEquals("50", basis.pnlAt(BigDecimal("105"), isLong = true)?.signedRoiPercent)
            assertDecimalEquals("-50", basis.pnlAt(BigDecimal("105"), isLong = false)?.signedRoiPercent)
            assertDecimalEquals("1000", basis.zeroPriceRoiPercentCeiling())
            assertNull(resolveTpSlCalculationBasis("", "100", margin, null, 10))
        }
    }

    @Test
    fun roundedSubmittedPriceDrivesPercentPreview() {
        val basis = existingBasis(quantity = "3", margin = "100")!!
        val submittedPrice = targetPriceForRoiPercent(basis, "10", isLong = true, isTakeProfit = true, priceScale = 2)

        assertEquals("103.33", submittedPrice)
        assertResult(basis.pnlAt(BigDecimal(submittedPrice), isLong = true), pnl = "9.99", roiPercent = "9.99")
    }

    @Test
    fun fractionalInputsRetainPrecisionUntilPresentation() {
        val basis = resolveTpSlCalculationBasis("12.34567891", "12", "7.65432109", "0.33333333", 20)!!
        val result = basis.pnlAt(BigDecimal("12.98765432"), isLong = true)!!

        assertDecimalEquals("0.2139918011934153", result.signedPnl)
        assertEquals("2.80", result.signedRoiPercent.setScale(2, RoundingMode.HALF_UP).toPlainString())
    }

    @Test
    fun invalidExistingInputsNeverFallBackToPreOpenEstimate() {
        for (quantity in listOf("", "bad", "0")) {
            assertNull(resolveTpSlCalculationBasis("100", "100", "200", quantity, 10))
            assertNull(resolveTpSlConversionBasis("100", "100", "200", quantity, 10))
        }
        for (margin in listOf("", "bad", "0", "-1")) {
            assertNull(resolveTpSlCalculationBasis("100", "100", margin, "10", 10))
            assertNull(resolveTpSlConversionBasis("100", "100", margin, "10", 10))
        }
        assertNull(resolveTpSlCalculationBasis("", "100", "200", "10", 10))
        assertNull(resolveTpSlCalculationBasis("bad", "100", "200", "10", 10))
        assertNull(resolveTpSlCalculationBasis("0", "100", "200", "10", 10))
    }

    @Test
    fun invalidPreOpenInputsDoNotProduceABasis() {
        assertNull(resolveTpSlCalculationBasis("0", "0", "100", null, 10))
        assertNull(resolveTpSlCalculationBasis("100", "100", "100", null, 0))
        assertNull(resolveTpSlCalculationBasis("100", "100", "-1", null, 10))
        assertNull(resolveTpSlConversionBasis("100", "100", "-1", null, 10))
        assertNull(resolveTpSlConversionBasis("100", "100", "bad", null, 10))
    }

    @Test
    fun preOpenQuantityRoundedToZeroDoesNotProduceABasis() {
        assertNull(resolveTpSlCalculationBasis("100000000000000000", "1", "1", null, 1))
    }

    @Test
    fun zeroPriceRoiCeilingUsesActualExistingPositionBasis() {
        val basis = existingBasis(quantity = "10", margin = "200")!!

        assertDecimalEquals("500", basis.zeroPriceRoiPercentCeiling())
    }

    @Test
    fun invalidOrUnchangedTargetDoesNotProducePnl() {
        val basis = existingBasis(quantity = "10", margin = "200")!!

        assertNull(basis.pnlAt(BigDecimal.ZERO, isLong = true))
        assertNull(basis.pnlAt(BigDecimal("100"), isLong = true))
    }

    @Test
    fun validServerLiquidationIsAuthoritativeAndStrict() {
        val longBound = resolveTpSlLiquidationBound("80", "100", "100", leverage = 10, isLong = true)
        val longBoundAtDifferentLeverage = resolveTpSlLiquidationBound("80", "100", "100", leverage = 50, isLong = true)
        val shortBound = resolveTpSlLiquidationBound("120", "100", "100", leverage = 50, isLong = false)

        assertDecimalEquals("80", longBound)
        assertDecimalEquals("80", longBoundAtDifferentLeverage)
        assertFalse(isStopBeyondLiquidation(BigDecimal("80"), longBound, isLong = true))
        assertFalse(isStopBeyondLiquidation(BigDecimal("79.99"), longBound, isLong = true))
        assertTrue(isStopBeyondLiquidation(BigDecimal("80.01"), longBound, isLong = true))
        assertDecimalEquals("120", shortBound)
        assertFalse(isStopBeyondLiquidation(BigDecimal("120"), shortBound, isLong = false))
        assertFalse(isStopBeyondLiquidation(BigDecimal("120.01"), shortBound, isLong = false))
        assertTrue(isStopBeyondLiquidation(BigDecimal("119.99"), shortBound, isLong = false))
    }

    @Test
    fun absentServerLiquidationUsesLegacyEstimateOnly() {
        assertDecimalEquals("90.0", resolveTpSlLiquidationBound(null, "100", "90", 10, isLong = true))
        assertDecimalEquals("110.0", resolveTpSlLiquidationBound("", "100", "90", 10, isLong = false))
        assertNull(resolveTpSlLiquidationBound("bad", "100", "90", 10, isLong = true))
        assertNull(resolveTpSlLiquidationBound("0", "100", "90", 10, isLong = true))
    }

    private fun existingBasis(quantity: String, margin: String): TpSlCalculationBasis? =
        resolveTpSlCalculationBasis("100", "100", margin, quantity, 10)

    private fun assertResult(result: TpSlPnlResult?, pnl: String, roiPercent: String) {
        requireNotNull(result)
        assertDecimalEquals(pnl, result.signedPnl)
        assertDecimalEquals(roiPercent, result.signedRoiPercent)
    }

    private fun assertDecimalEquals(expected: String, actual: BigDecimal?) {
        requireNotNull(actual)
        assertEquals(0, BigDecimal(expected).compareTo(actual))
    }
}
