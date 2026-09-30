package one.mixin.android.ui.home.web3.trade.perps

import com.google.gson.JsonParser
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PerpsMarginAmountTest {
    @Test
    fun fractionalPercentageLimitCanBeFilledAsDollars() {
        val maximum = BigDecimal("0.056")
        assertEquals("0.56%", formatPerpsMarginLimit("9.97", maximum, true))
        val isPercentage = canFillMarginReductionAsPercentage("9.97", maximum)
        assertFalse(isPercentage)
        val input = maximumMarginReductionInput("9.97", maximum, isPercentage)
        assertEquals("0.05", input)
        val reduction = requireNotNull(reduceMarginAmount("9.97", input, isPercentage))
        assertTrue(reduction <= maximum)
        assertEquals(BigDecimal("9.92"), marginAfterAdjustment("9.97", input, false, maximum))
        assertFalse(canFillMarginReductionAsPercentage("6.98", BigDecimal("2.07")))
        assertTrue(canFillMarginReductionAsPercentage("100", BigDecimal("50.00")))
        assertTrue(canFillMarginReductionAsPercentage("100", BigDecimal.ZERO))
    }

    @Test
    fun fillsMaximumRemovalInTheCurrentUnitWithoutExceedingTheLimit() {
        assertEquals("50", maximumMarginReductionInput("100", BigDecimal("50"), true))
        assertEquals("50", maximumMarginReductionInput("100", BigDecimal("50"), false))
        assertEquals("29", maximumMarginReductionInput("6.98", BigDecimal("2.07"), true))
        assertEquals("2.07", maximumMarginReductionInput("6.98", BigDecimal("2.07999999"), false))
        for (isPercentage in listOf(true, false)) {
            assertEquals("0", maximumMarginReductionInput("100", BigDecimal.ZERO, isPercentage))
            val input = maximumMarginReductionInput("6.98", BigDecimal("2.07999999"), isPercentage)
            val reduction = requireNotNull(reduceMarginAmount("6.98", input, isPercentage))
            assertTrue(reduction <= BigDecimal("2.07999999"))
        }
    }

    @Test
    fun removableMarginErrorConvertsUnitsWithoutRoundingAboveTheLimit() {
        assertEquals("29.65%", formatPerpsMarginLimit("6.98", BigDecimal("2.07"), true))
        assertEquals("$2.07", formatPerpsMarginLimit("6.98", BigDecimal("2.07"), false))
        assertEquals("99.99%", formatPerpsMarginLimit("100", BigDecimal("99.99999999"), true))
        assertEquals("$2.07", formatPerpsMarginLimit("6.98", BigDecimal("2.079999999"), false))
        assertEquals("$1.12", formatPerpsMarginLimit("10.93", BigDecimal("1.12139433"), false))
        assertEquals("1.12", maximumMarginReductionInput("10.93", BigDecimal("1.12139433"), false))
        assertEquals("$0", formatPerpsMarginLimit("1", BigDecimal("0.00000001"), false))
        assertEquals("0%", formatPerpsMarginLimit("6.98", BigDecimal.ZERO, true))
        assertEquals("$0", formatPerpsMarginLimit("6.98", BigDecimal.ZERO, false))
        assertEquals("0%", formatPerpsMarginLimit("0", BigDecimal.ZERO, true))
    }

    @Test
    fun marginPreviewKeepsCurrencyForZeroAndTheActualLiquidationPrice() {
        listOf(null, BigDecimal.ZERO, BigDecimal("0.00"), BigDecimal("0.004")).forEach {
            assertEquals("$0", formatPerpsMarginAmount(it))
        }
        assertEquals("$1.50", formatPerpsMarginAmount(BigDecimal("1.5")))
        assertEquals(BigDecimal("130"), marginAfterAdjustment("100", "30", true))
        assertEquals(BigDecimal("12.50"), marginLiquidationLossPercent("80000", "70000"))
        assertEquals(BigDecimal("12.50"), marginLiquidationLossPercent("80000", "90000"))
        assertNull(marginLiquidationLossPercent("0", "70000"))
        assertNull(marginLiquidationLossPercent("80000", null))
    }

    @Test
    fun inputKeepsWholePercentagesAndTwoDollarDecimalsWithoutExceedingTheAmount() {
        assertEquals("0", formatMarginAdjustmentInput(BigDecimal.ZERO, true))
        assertEquals("100", formatMarginAdjustmentInput(BigDecimal("100"), true))
        assertEquals("0", formatMarginAdjustmentInput(BigDecimal.ZERO, false))
        assertEquals("0", formatMarginAdjustmentInput(BigDecimal("0.009"), false))
        assertEquals("1.5", formatMarginAdjustmentInput(BigDecimal("1.5"), false))
        assertEquals("2", formatMarginAdjustmentInput(BigDecimal.ONE + BigDecimal.ONE, false))
        assertEquals("1", formatMarginAdjustmentInput(BigDecimal("2.00") - BigDecimal.ONE, false))
        assertEquals("2.5", formatMarginAdjustmentInput(BigDecimal("1.50") + BigDecimal.ONE, false))
        assertEquals("5.01", formatMarginAdjustmentInput(BigDecimal("5.01253097"), false))
        assertEquals("0.99", formatMarginAdjustmentInput(BigDecimal("0.999"), false))
    }

    @Test
    fun sliderSnapsNearQuarterMarksAndKeepsOtherWholePercentages() {
        for (marker in 0..100 step 25) {
            for (offset in -3..3) {
                assertEquals(marker, snapMarginReductionPercentage((marker + offset).toFloat()))
            }
        }
        listOf(4, 21, 29, 46, 54, 71, 79, 96).forEach {
            assertEquals(it, snapMarginReductionPercentage(it.toFloat()))
        }
        assertEquals(42, snapMarginReductionPercentage(41.6f))
    }

    @Test
    fun acceptsOnlyPositiveDecimalInputWithinPrecisionLimit() {
        assertEquals(BigDecimal("0.00000001"), marginAdjustmentAmount("0.00000001"))
        assertEquals(BigDecimal("0.5"), marginAdjustmentAmount(".5"))
        listOf("1e8", "1e100000000", "-1", "+1", "1.2.3", "0.000000001", "9".repeat(41), "NaN", "", "0").forEach {
            assertNull(marginAdjustmentAmount(it))
        }
    }

    @Test
    fun adjustsMarginWithoutLosingDecimalPrecision() {
        assertEquals(BigDecimal("10.00000002"), marginAfterAdjustment("10.00000001", "0.00000001", true))
        assertEquals(BigDecimal("10.00000000"), marginAfterAdjustment("10.00000001", "0.00000001", false))
    }

    @Test
    fun reducesByPercentageOfCurrentMarginAndRefreshesTheAmount() {
        assertEquals(BigDecimal("25.00000000"), reduceMarginAmount("100", "25", true))
        assertEquals(BigDecimal("20.00000000"), reduceMarginAmount("80", "25", true))
        assertEquals(BigDecimal("25"), reduceMarginAmount("80", "25", false))
        assertEquals(BigDecimal("25.00000000"), marginReductionPercentage("80", BigDecimal("20")))
    }

    @Test
    fun percentageConversionRoundsDownToEightDecimals() {
        val reduction = reduceMarginAmount("1.00000001", "33", true)
        assertEquals(BigDecimal("0.33000000"), reduction)
        assertEquals(BigDecimal("0.67000001"), marginAfterAdjustment("1.00000001", reduction!!.toPlainString(), false))
        assertNull(reduceMarginAmount("0.00000001", "25", true))
    }

    @Test
    fun percentageRequiresWholeNumbersWhileAmountKeepsDecimals() {
        listOf("81.88", "100.0", ".5").forEach {
            assertNull(reduceMarginAmount("100", it, true))
            assertEquals(BigDecimal(it), reduceMarginAmount("100", it, false))
        }
        assertEquals(BigDecimal("5.01253097"), reduceMarginAmount("5.01253097", "100", true))
    }

    @Test
    fun invalidReductionDoesNotChangeTheMargin() {
        listOf("", "0", "-1", "invalid").forEach {
            assertNull(reduceMarginAmount("100", it, true))
        }
        val excessive = reduceMarginAmount("100", "101", true)
        assertNull(marginAfterAdjustment("100", excessive!!.toPlainString(), false))
        assertNull(reduceMarginAmount(null, "25", true))
        assertNull(marginReductionPercentage("0", BigDecimal.ONE))
    }

    @Test
    fun reductionPreviewRequiresAmountWithinAvailableMargin() {
        assertNull(marginAfterAdjustment("100", "60", false, BigDecimal("50")))
        assertNull(marginAfterAdjustment("100", "0.00000001", false, BigDecimal.ZERO))
        assertEquals(BigDecimal("75"), marginAfterAdjustment("100", "25", false, BigDecimal("50")))
        assertEquals(BigDecimal("50"), marginAfterAdjustment("100", "50", false, BigDecimal("50")))
        assertEquals(BigDecimal("160"), marginAfterAdjustment("100", "60", true, BigDecimal("50")))
    }

    @Test
    fun rejectsInvalidAmountsAndNegativeRemainingMargin() {
        listOf("", "invalid", "0", "-1").forEach { amount ->
            assertNull(marginAfterAdjustment("10", amount, true))
            assertNull(marginAfterAdjustment("10", amount, false))
        }
        assertNull(marginAfterAdjustment("10", "10.00000001", false))
        assertNull(marginAfterAdjustment("invalid", "1", true))
        assertNull(marginAfterAdjustment(null, "1", true))
        assertNull(marginAfterAdjustment("-1", "2", true))
    }

    @Test
    fun readsAvailableMarginFromDecimalStringOrNumber() {
        assertEquals(BigDecimal("1.00000001"), availableMarginFromError(JsonParser.parseString("""{"available_margin":"1.00000001"}""")))
        assertEquals(BigDecimal("1.25"), availableMarginFromError(JsonParser.parseString("""{"available_margin":1.25}""")))
        assertEquals(BigDecimal.ZERO, availableMarginFromError(JsonParser.parseString("""{"available_margin":"0"}""")))
    }

    @Test
    fun ignoresMissingOrMalformedAvailableMargin() {
        assertNull(availableMarginFromError(null))
        listOf("null", "[]", "{}", """{"available_margin":null}""", """{"available_margin":{}}""", """{"available_margin":true}""", """{"available_margin":"NaN"}""", """{"available_margin":"-1"}""").forEach {
            assertNull(availableMarginFromError(JsonParser.parseString(it)))
        }
    }
}
