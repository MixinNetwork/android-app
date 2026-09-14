package one.mixin.android.ui.home.web3.trade.perps

import com.google.gson.JsonParser
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PerpsMarginAmountTest {
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
        val reduction = reduceMarginAmount("1", "33.33333333", true)
        assertEquals(BigDecimal("0.33333333"), reduction)
        assertEquals(BigDecimal("0.66666667"), marginAfterAdjustment("1", reduction!!.toPlainString(), false))
        assertNull(reduceMarginAmount("0.00000001", "25", true))
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
