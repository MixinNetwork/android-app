package one.mixin.android.web3.details

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import one.mixin.android.db.web3.vo.AssetChange
import one.mixin.android.db.web3.vo.TransactionStatus
import one.mixin.android.db.web3.vo.TransactionType
import one.mixin.android.db.web3.vo.Web3TransactionItem

class Web3TransactionDetailStateTest {
    @Test
    fun refreshedSuccessUpdatesIncomingAmountTone() {
        val pendingState = Web3TransactionDetailState(
            status = TransactionStatus.PENDING.value,
            transactionType = TransactionType.TRANSFER_IN.value,
        )

        assertEquals(Web3TransactionAmountTone.MINOR, pendingState.amountTone)

        val refreshedState = pendingState.withStatus(TransactionStatus.SUCCESS.value)

        assertEquals(TransactionStatus.SUCCESS.value, refreshedState.status)
        assertEquals(Web3TransactionAmountTone.INCOMING, refreshedState.amountTone)
    }

    @Test
    fun refreshedSuccessUpdatesOutgoingAmountTone() {
        val pendingState = Web3TransactionDetailState(
            status = TransactionStatus.PENDING.value,
            transactionType = TransactionType.TRANSFER_OUT.value,
        )

        val refreshedState = pendingState.withStatus(TransactionStatus.SUCCESS.value)

        assertEquals(Web3TransactionAmountTone.OUTGOING, refreshedState.amountTone)
    }
    @Test
    fun unavailableStatusesKeepBothDirectionsNeutral() {
        for (status in listOf(TransactionStatus.PENDING, TransactionStatus.FAILED, TransactionStatus.NOT_FOUND)) {
            for (type in listOf(TransactionType.TRANSFER_IN, TransactionType.TRANSFER_OUT)) {
                assertEquals(Web3TransactionAmountTone.MINOR, Web3TransactionDetailState(status.value, type.value, "12").amountTone)
            }
        }
    }

    @Test
    fun zeroIsNeutralAndUnsignedWhileDirectionsNormalizeExistingSigns() {
        for (amount in listOf("0", "-0.00", "+0.00")) {
            assertEquals(Web3TransactionAmountTone.MINOR, Web3TransactionDetailState(TransactionStatus.SUCCESS.value, TransactionType.TRANSFER_IN.value, amount).amountTone)
            assertEquals(amount.removePrefix("+").removePrefix("-"), formatWeb3AmountWithSign(amount, true))
            assertEquals(amount.removePrefix("+").removePrefix("-"), formatWeb3AmountWithSign(amount, false))
        }
        assertEquals("+1,234.5", formatWeb3AmountWithSign("-1,234.5", true))
        assertEquals("-12", formatWeb3AmountWithSign("+12", false))
    }
    @Test
    fun missingAssetChangesAreDistinctFromAnActualZeroAmount() {
        val transaction = Web3TransactionItem(
            transactionHash = "hash",
            transactionType = TransactionType.TRANSFER_IN.value,
            status = TransactionStatus.FAILED.value,
            blockNumber = 0,
            chainId = "chain",
            address = "address",
            fee = "0",
            senders = emptyList(),
            receivers = emptyList(),
            transactionAt = "2026-09-08T00:00:00Z",
            updatedAt = "2026-09-08T00:00:00Z",
            level = 0,
        )
        val zero = AssetChange(assetId = "asset", amount = "0")
        assertFalse(transaction.hasDisplayAssetChanges())
        assertTrue(transaction.copy(receivers = listOf(zero)).hasDisplayAssetChanges())
        assertFalse(transaction.copy(senders = listOf(zero)).hasDisplayAssetChanges())
        assertTrue(transaction.copy(transactionType = TransactionType.TRANSFER_OUT.value, senders = listOf(zero)).hasDisplayAssetChanges())
        assertTrue(transaction.copy(transactionType = TransactionType.UNKNOWN.value, receivers = listOf(zero)).hasDisplayAssetChanges())
    }
}
