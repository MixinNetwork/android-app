package one.mixin.android.ui.common

import kotlin.test.Test
import kotlin.test.assertEquals

class BalanceChangePresentationTest {
    @Test
    fun receiveAmountUsesPositiveTone() {
        val presentation = balanceChangePresentation(
            amount = "1.25",
            isReceive = true,
        )

        assertEquals("+1.25", presentation.amount)
        assertEquals(BalanceChangeTone.POSITIVE, presentation.tone)
    }

    @Test
    fun signedAmountUsesMatchingSignAndTone() {
        assertEquals(
            BalanceChangePresentation("+1.25", BalanceChangeTone.POSITIVE),
            balanceChangePresentation("1.25"),
        )
        assertEquals(
            BalanceChangePresentation("-1.25", BalanceChangeTone.NEGATIVE),
            balanceChangePresentation("-1.25"),
        )
        assertEquals(
            BalanceChangePresentation("0", BalanceChangeTone.PLAIN),
            balanceChangePresentation("+0"),
        )
    }

    @Test
    fun forcedSignDoesNotDuplicateExistingSign() {
        assertEquals("+1.25", balanceChangePresentation("-1.25", isReceive = true).amount)
        assertEquals("-1.25", balanceChangePresentation("+1.25", isReceive = false).amount)
    }
}
