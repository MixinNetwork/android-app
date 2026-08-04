package one.mixin.android.ui.wallet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CashAccountQuotePrecheckTest {
    @Test
    fun returnsBelowMinimumWhenEstimatedValueIsBelowMinimumReceiveAmount() {
        assertEquals(
            CashAccountQuotePrecheckError.BELOW_MINIMUM_RECEIVE,
            cashAccountQuotePrecheckError("0.01", "5", "0.1"),
        )
    }

    @Test
    fun returnsUnsupportedTokenWhenTokenPriceIsMissing() {
        assertEquals(
            CashAccountQuotePrecheckError.UNSUPPORTED_TOKEN,
            cashAccountQuotePrecheckError("1", null, "0.1"),
        )
        assertEquals(
            CashAccountQuotePrecheckError.UNSUPPORTED_TOKEN,
            cashAccountQuotePrecheckError("1", "0", "0.1"),
        )
    }

    @Test
    fun returnsUnsupportedTokenWhenInitialAmountHasMissingTokenPrice() {
        assertEquals(
            CashAccountQuotePrecheckError.UNSUPPORTED_TOKEN,
            cashAccountQuotePrecheckError("0", null, "0.1"),
        )
        assertEquals(
            CashAccountQuotePrecheckError.UNSUPPORTED_TOKEN,
            cashAccountQuotePrecheckError("0", "0", "0.1"),
        )
    }

    @Test
    fun returnsNullWhenInitialAmountHasTokenPrice() {
        assertNull(cashAccountQuotePrecheckError("0", "5", "0.1"))
    }

    @Test
    fun returnsNullWhenEstimatedValueMeetsMinimumReceiveAmount() {
        assertNull(cashAccountQuotePrecheckError("0.02", "5", "0.1"))
    }
}
