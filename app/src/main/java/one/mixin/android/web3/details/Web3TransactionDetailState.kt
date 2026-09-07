package one.mixin.android.web3.details

import one.mixin.android.db.web3.vo.TransactionStatus
import one.mixin.android.db.web3.vo.TransactionType

internal data class Web3TransactionDetailState(
    val status: String,
    val transactionType: String,
) {
    val amountTone: Web3TransactionAmountTone
        get() = when {
            status == TransactionStatus.PENDING.value ||
                status == TransactionStatus.NOT_FOUND.value ||
                status == TransactionStatus.FAILED.value -> Web3TransactionAmountTone.ASSIST
            transactionType == TransactionType.TRANSFER_OUT.value -> Web3TransactionAmountTone.OUTGOING
            transactionType == TransactionType.TRANSFER_IN.value -> Web3TransactionAmountTone.INCOMING
            else -> Web3TransactionAmountTone.PRIMARY
        }

    fun withStatus(status: String): Web3TransactionDetailState = copy(status = status)
}

internal enum class Web3TransactionAmountTone {
    ASSIST,
    OUTGOING,
    INCOMING,
    PRIMARY,
}
