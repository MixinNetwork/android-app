package one.mixin.android.ui.home.web3.trade.perps

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class PerpsMarginInputTest {
    @Test
    fun maximumBelowOneCentKeepsItsPrecision() {
        val maximum = java.math.BigDecimal("0.005")
        assertEquals("0.005", maximumMarginReductionInput("10", maximum, false))
        assertEquals("$0.005", formatPerpsMarginLimit("10", maximum, false))
        assertEquals(maximum, reduceMarginAmount("10", "0.005", false))
        assertEquals("1.23", maximumMarginReductionInput("10", java.math.BigDecimal("1.239"), false))
    }

    @Test
    fun externalAmountChangesMoveCursorToEndAndManualEditsKeepSelection() {
        val editing = TextFieldValue("25", TextRange(1), TextRange(0, 2))
        assertSame(editing, syncPerpsMarginInput(editing, "25"))
        for (input in listOf("50", "100", "5.00", "")) {
            assertEquals(TextFieldValue(input, TextRange(input.length)), syncPerpsMarginInput(editing, input))
        }
    }
}
