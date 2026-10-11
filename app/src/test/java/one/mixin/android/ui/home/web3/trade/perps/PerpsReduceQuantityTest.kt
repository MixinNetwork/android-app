package one.mixin.android.ui.home.web3.trade.perps

import com.google.gson.Gson
import one.mixin.android.api.request.perps.CloseOrderRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class PerpsReduceQuantityTest {
    @Test
    fun percentageReducesAbsoluteQuantityWithoutUsingMarginOrLeverage() {
        assertDecimal("0.25", perpsReductionQuantity("1", "88.23", "25", true, 8))
        assertDecimal("0.25", perpsReductionQuantity("-1", "88.23", "25", true, 8))
        assertDecimal("0.2", perpsReductionQuantity("0.8", "88.23", "25", true, 8))
        assertNull(perpsReductionQuantity("0.00000001", "1", "25", true, 8))
        assertDecimal("1.00000001", perpsReductionQuantity("1.00000001", "88.23", "100", true, 8))
        assertNull(perpsReductionQuantity("1", "88.23", "101", true, 8))
        assertNull(perpsReductionQuantity("1", "88.23", "25.5", true, 8))
    }

    @Test
    fun dollarInputUsesPositionValueAndNeverRoundsAboveTheBudget() {
        assertDecimal("0.2", perpsReductionQuantity("1", "100", "20", false, 8))
        val quantity = requireNotNull(perpsReductionQuantity("10", "3", "1", false, 8))
        assertDecimal("0.33333333", quantity)
        assertTrue(quantity.multiply(BigDecimal("3")) <= BigDecimal.ONE)
        assertNull(perpsReductionQuantity("1", "100", "100.01", false, 8))
        assertNull(perpsReductionQuantity("1", "0", "1", false, 8))
        assertNull(perpsReductionQuantity("1", null, "1", false, 8))
    }

    @Test
    fun reductionsRoundDownToMarketQuantityScale() {
        assertDecimal("0.002", perpsReductionQuantity("0.011", "100", "25", true, 3))
        assertDecimal("0.003", perpsReductionQuantity("1", "300", "1", false, 3))
        assertDecimal("0.011", perpsReductionQuantity("0.011", "100", "100", true, 3))
        assertDecimal("2", perpsReductionQuantity("11", "100", "25", true, 0))
        assertNull(perpsReductionQuantity("0.001", "100", "25", true, 3))
        assertNull(perpsReductionQuantity("1", "300", "0.1", false, 3))
        assertNull(perpsReductionQuantity("1", "100", "25", true, -1))
    }

    @Test
    fun quantityIsRevalidatedAgainstChangedPositionAndNeverTurnsIntoFullClose() {
        assertDecimal("0.5", perpsReduceQuantity("0.5", "1"))
        assertNull(perpsReduceQuantity("0.5", "0.4"))
        assertNull(perpsReduceQuantity("0.5", "0"))
        assertDecimal("0.5", perpsReduceQuantity("0.5", "-0.5"))
        for (input in listOf("", ".", "-1", "+1", "0", "1e2", "NaN", "1.2.3", "9".repeat(41))) {
            assertNull(perpsReduceQuantity(input, "100"))
            assertNull(perpsReductionQuantity("100", "1", input, true, 8))
            assertNull(perpsReductionQuantity("100", "1", input, false, 8))
        }
    }

    @Test
    fun estimatesOnlyTheReducedShareAndPreservesPnlSign() {
        assertDecimal("5", perpsReductionValue("20", "0.25", "1"))
        assertDecimal("-5", perpsReductionValue("-20", "0.25", "-1"))
        assertDecimal("30", perpsReductionValue("120", "0.25", "1"))
        assertDecimal("120", perpsReductionValue("120", "1", "1"))
        assertNull(perpsReductionValue("120", "1", "0.5"))
        assertNull(perpsReductionValue("120", "1", "0"))
        assertNull(perpsReductionValue(null, "1", "1"))
    }

    @Test
    fun requestSerializesExplicitQuantityAndOmitsItForExistingFullClose() {
        val gson = Gson()
        val partial = gson.toJsonTree(CloseOrderRequest(positionId = "position", quantity = "0.00000001")).asJsonObject
        assertEquals("position", partial["position_id"].asString)
        assertEquals("0.00000001", partial["quantity"].asString)
        assertFalse(partial.has("price"))
        assertEquals(2, partial.size())
        val full = gson.toJsonTree(CloseOrderRequest("position")).asJsonObject
        assertFalse(full.has("quantity"))
        assertEquals(1, full.size())
    }

    private fun assertDecimal(expected: String, actual: BigDecimal?) {
        assertEquals(expected, actual?.stripTrailingZeros()?.toPlainString())
    }
}
