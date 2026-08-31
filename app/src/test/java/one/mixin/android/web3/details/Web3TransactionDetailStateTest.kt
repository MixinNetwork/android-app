package one.mixin.android.web3.details

import one.mixin.android.db.web3.vo.TransactionStatus
import one.mixin.android.db.web3.vo.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals

class Web3TransactionDetailStateTest {
    @Test
    fun refreshedSuccessUpdatesIncomingAmountTone() {
        val pendingState = Web3TransactionDetailState(
            status = TransactionStatus.PENDING.value,
            transactionType = TransactionType.TRANSFER_IN.value,
        )

        assertEquals(Web3TransactionAmountTone.ASSIST, pendingState.amountTone)

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
}
