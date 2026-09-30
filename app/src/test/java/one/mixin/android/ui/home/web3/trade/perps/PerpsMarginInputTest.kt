package one.mixin.android.ui.home.web3.trade.perps

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class PerpsMarginInputTest {
    @Test
    fun externalAmountChangesMoveCursorToEndAndManualEditsKeepSelection() {
        val editing = TextFieldValue("25", TextRange(1), TextRange(0, 2))
        assertSame(editing, syncPerpsMarginInput(editing, "25"))
        for (input in listOf("50", "100", "5.00", "")) {
            assertEquals(TextFieldValue(input, TextRange(input.length)), syncPerpsMarginInput(editing, input))
        }
    }
}
