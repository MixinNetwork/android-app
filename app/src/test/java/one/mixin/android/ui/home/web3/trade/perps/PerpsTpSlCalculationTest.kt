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
    fun preOpenWithoutMarginDoesNotProduceABasis() {
        for (margin in listOf("", "0", "0.00")) {
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
        }
        for (margin in listOf("", "bad", "0", "-1")) {
            assertNull(resolveTpSlCalculationBasis("100", "100", margin, "10", 10))
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
        assertNull(resolveTpSlCalculationBasis("100", "100", "bad", null, 10))
    }

    @Test
    fun preOpenQuantityRoundedToZeroDoesNotProduceABasis() {
        assertNull(resolveTpSlCalculationBasis("100000000000000000", "1", "1", null, 1))
    }

    @Test
    fun zeroPriceRoiCeilingUsesActualExistingPositionBasis() {
        val basis = existingBasis(quantity = "10", margin = "200")!!

        assertDecimalEquals("500", basis.zeroPriceRoiPercentCeiling(isLong = true))
        assertDecimalEquals("500", basis.zeroPriceRoiPercentCeiling(isLong = false))
    }

    @Test
    fun existingPositionUsesOpenAndCloseFeesInPnlAndTargetPrice() {
        val basis = existingBasis(quantity = "10", margin = "200", estimatedCloseFee = "1", feeReferencePrice = "100")!!

        assertDecimalEquals("0.001", basis.feeRate)
        assertResult(basis.pnlAt(BigDecimal("110"), isLong = true), pnl = "97.9", roiPercent = "48.95")
        assertEquals("110.2102102102102102", targetPriceForRoiPercent(basis, "50", isLong = true, isTakeProfit = true, priceScale = 16))
        assertEquals("90.1901901901901902", targetPriceForRoiPercent(basis, "50", isLong = true, isTakeProfit = false, priceScale = 16))
        assertEquals("89.8101898101898102", targetPriceForRoiPercent(basis, "50", isLong = false, isTakeProfit = true, priceScale = 16))
        assertEquals("109.7902097902097902", targetPriceForRoiPercent(basis, "50", isLong = false, isTakeProfit = false, priceScale = 16))
        assertDecimalEquals("500.5", basis.zeroPriceRoiPercentCeiling(isLong = true))
        assertDecimalEquals("499.5", basis.zeroPriceRoiPercentCeiling(isLong = false))
    }

    @Test
    fun existingPositionFeeRoundsBackToRequestedRoi() {
        val basis = existingBasis(quantity = "10", margin = "200", estimatedCloseFee = "1", feeReferencePrice = "100")!!

        for ((isLong, isTakeProfit) in listOf(true to true, true to false, false to true, false to false)) {
            val price = targetPriceForRoiPercent(basis, "50", isLong, isTakeProfit, priceScale = 16)
            val result = basis.pnlAt(BigDecimal(price), isLong)!!
            val expectedPnl = if (isTakeProfit) "100" else "-100"
            assertDecimalEquals(expectedPnl, result.signedPnl.setScale(8, RoundingMode.HALF_UP))
            assertDecimalEquals(if (isTakeProfit) "50" else "-50", result.signedRoiPercent.setScale(8, RoundingMode.HALF_UP))
        }
    }

    @Test
    fun profitableLongStopLossKeepsProfitAfterFees() {
        val basis = existingBasis(
            quantity = "10",
            margin = "200",
            currentPrice = "200",
            estimatedCloseFee = "2",
        )!!

        assertResult(basis.pnlAt(BigDecimal("150"), isLong = true), pnl = "497.5", roiPercent = "248.75")
    }

    @Test
    fun profitableShortStopLossKeepsProfitAfterFees() {
        val basis = existingBasis(
            quantity = "-10",
            margin = "200",
            currentPrice = "50",
            estimatedCloseFee = "0.5",
        )!!

        assertResult(basis.pnlAt(BigDecimal("75"), isLong = false), pnl = "248.25", roiPercent = "124.125")
    }

    @Test
    fun takeProfitBoundRequiresProfitAfterFeesAndFutureTrigger() {
        val basis = existingBasis(quantity = "10", margin = "200", estimatedCloseFee = "1")!!

        assertDecimalEquals("100.2002002002002002", basis.takeProfitPriceBound(BigDecimal("90"), isLong = true))
        assertDecimalEquals("200", basis.takeProfitPriceBound(BigDecimal("200"), isLong = true))
        assertDecimalEquals("99.8001998001998002", basis.takeProfitPriceBound(BigDecimal("110"), isLong = false))
        assertDecimalEquals("50", basis.takeProfitPriceBound(BigDecimal("50"), isLong = false))
    }

    @Test
    fun tinyTakeProfitRoundsToProfitablePriceAfterFees() {
        val longBasis = existingBasis(quantity = "10", margin = "0.01", estimatedCloseFee = "1")!!
        val shortBasis = existingBasis(
            quantity = "10",
            margin = "0.01",
            entryPrice = "101",
            currentPrice = "101",
            estimatedCloseFee = "1.01",
        )!!

        val longPrice = targetPriceForRoiPercent(longBasis, "0.01", isLong = true, isTakeProfit = true, priceScale = 2)
        val shortPrice = targetPriceForRoiPercent(shortBasis, "0.01", isLong = false, isTakeProfit = true, priceScale = 2)

        assertEquals("100.21", longPrice)
        assertEquals("100.79", shortPrice)
        assertTrue(longBasis.pnlAt(BigDecimal(longPrice), isLong = true)!!.signedPnl > BigDecimal.ZERO)
        assertTrue(shortBasis.pnlAt(BigDecimal(shortPrice), isLong = false)!!.signedPnl > BigDecimal.ZERO)
    }

    @Test
    fun tinyTakeProfitDoesNotRoundToZeroProfit() {
        val basis = existingBasis(quantity = "10", margin = "0.01")!!

        assertEquals("100.01", targetPriceForRoiPercent(basis, "0.01", isLong = true, isTakeProfit = true, priceScale = 2))
        assertEquals("99.99", targetPriceForRoiPercent(basis, "0.01", isLong = false, isTakeProfit = true, priceScale = 2))
    }

    @Test
    fun missingOrUnusableCloseFeeKeepsGrossPnl() {
        for (fee in listOf(null, "", "0", "bad", "-1")) {
            val basis = existingBasis(quantity = "10", margin = "200", estimatedCloseFee = fee, feeReferencePrice = "100")!!
            assertEquals(0, BigDecimal.ZERO.compareTo(basis.feeRate))
            assertEquals("110", targetPriceForRoiPercent(basis, "50", isLong = true, isTakeProfit = true, priceScale = 2))
        }
        val extremeFee = existingBasis(quantity = "10", margin = "200", estimatedCloseFee = "10000", feeReferencePrice = "100")!!
        assertEquals(0, BigDecimal.ZERO.compareTo(extremeFee.feeRate))
        val preOpen = resolveTpSlCalculationBasis(
            entryPrice = "100",
            currentPrice = "100",
            marginAmount = "100",
            positionQuantity = null,
            leverage = 10,
            estimatedCloseFee = "1",
            feeReferencePrice = "100",
        )!!
        assertEquals(0, BigDecimal.ZERO.compareTo(preOpen.feeRate))
        assertEquals("105", targetPriceForRoiPercent(preOpen, "50", isLong = true, isTakeProfit = true, priceScale = 2))
    }

    @Test
    fun invalidTargetDoesNotProducePnlAndBreakEvenKeepsZero() {
        val basis = existingBasis(quantity = "10", margin = "200")!!

        assertNull(basis.pnlAt(BigDecimal.ZERO, isLong = true))
        assertResult(basis.pnlAt(BigDecimal("100"), isLong = true), pnl = "0", roiPercent = "0")

        val feeBasis = existingBasis(
            quantity = "10",
            margin = "200",
            entryPrice = "99.9",
            currentPrice = "99.9",
            estimatedCloseFee = "0.999",
        )!!
        assertResult(feeBasis.pnlAt(BigDecimal("100.1"), isLong = true), pnl = "0", roiPercent = "0")
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

    private fun existingBasis(
        quantity: String,
        margin: String,
        estimatedCloseFee: String? = null,
        feeReferencePrice: String? = null,
        entryPrice: String = "100",
        currentPrice: String = "100",
    ): TpSlCalculationBasis? = resolveTpSlCalculationBasis(
        entryPrice = entryPrice,
        currentPrice = currentPrice,
        marginAmount = margin,
        positionQuantity = quantity,
        leverage = 10,
        estimatedCloseFee = estimatedCloseFee,
        feeReferencePrice = feeReferencePrice,
    )

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
