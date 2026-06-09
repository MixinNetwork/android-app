package one.mixin.android.ui.wallet.home

import kotlin.test.Test
import kotlin.test.assertEquals

class WalletHomePendingIndicatorTest {
    @Test
    fun rawPendingTransactionsAreShownBeforeSyncedTransactions() {
        assertEquals(
            1,
            walletHomePendingTransactionCount(
                rawPendingCount = 1,
                transactionPendingCount = 0,
            ),
        )
    }

    @Test
    fun syncedPendingTransactionsRemainVisibleWhenRawTransactionsAreGone() {
        assertEquals(
            3,
            walletHomePendingTransactionCount(
                rawPendingCount = 0,
                transactionPendingCount = 3,
            ),
        )
    }

    @Test
    fun visiblePendingTransactionsAvoidCountingTheSameLocalTransactionTwice() {
        assertEquals(
            2,
            walletHomePendingTransactionCount(
                rawPendingCount = 2,
                transactionPendingCount = 1,
            ),
        )
    }
}
