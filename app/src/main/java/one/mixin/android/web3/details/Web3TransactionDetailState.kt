package one.mixin.android.web3.details

import android.content.Context
import one.mixin.android.R
import one.mixin.android.db.web3.vo.TransactionStatus
import one.mixin.android.db.web3.vo.TransactionType
import one.mixin.android.db.web3.vo.Web3TransactionItem
import one.mixin.android.extension.colorAttr

internal data class Web3TransactionDetailState(
    val status: String,
    val transactionType: String,
    val amount: String? = null,
) {
    val amountTone: Web3TransactionAmountTone
        get() = when {
            amount?.replace(",", "")?.toBigDecimalOrNull()?.signum() == 0 ||
                status == TransactionStatus.PENDING.value ||
                status == TransactionStatus.NOT_FOUND.value ||
                status == TransactionStatus.FAILED.value -> Web3TransactionAmountTone.MINOR
            transactionType == TransactionType.TRANSFER_OUT.value -> Web3TransactionAmountTone.OUTGOING
            transactionType == TransactionType.TRANSFER_IN.value -> Web3TransactionAmountTone.INCOMING
            else -> Web3TransactionAmountTone.PRIMARY
        }

    fun withStatus(status: String): Web3TransactionDetailState = copy(status = status)
}

internal enum class Web3TransactionAmountTone {
    MINOR,
    OUTGOING,
    INCOMING,
    PRIMARY,
}

internal fun formatWeb3AmountWithSign(amount: String, positive: Boolean): String {
    val magnitude = amount.removePrefix("+").removePrefix("-")
    if (magnitude.replace(",", "").toBigDecimalOrNull()?.signum() == 0) return magnitude
    return if (positive) "+$magnitude" else "-$magnitude"
}

internal fun Context.web3AmountColor(status: String, amount: String, isReceive: Boolean): Int =
    when (Web3TransactionDetailState(status, if (isReceive) TransactionType.TRANSFER_IN.value else TransactionType.TRANSFER_OUT.value, amount).amountTone) {
        Web3TransactionAmountTone.MINOR -> colorAttr(R.attr.text_minor)
        Web3TransactionAmountTone.OUTGOING -> getColor(R.color.wallet_pink)
        Web3TransactionAmountTone.INCOMING -> getColor(R.color.wallet_green)
        Web3TransactionAmountTone.PRIMARY -> colorAttr(R.attr.text_primary)
    }

internal fun Web3TransactionItem.hasDisplayAssetChanges(): Boolean = when (transactionType) {
    TransactionType.TRANSFER_IN.value -> receivers.isNotEmpty()
    TransactionType.TRANSFER_OUT.value -> senders.isNotEmpty()
    TransactionType.APPROVAL.value -> !approvals.isNullOrEmpty()
    else -> receivers.isNotEmpty() || senders.isNotEmpty()
}
