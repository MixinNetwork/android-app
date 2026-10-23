package one.mixin.android.web3.details

import androidx.core.view.isVisible
import one.mixin.android.R
import one.mixin.android.db.web3.vo.TransactionStatus
import one.mixin.android.db.web3.vo.TransactionType
import one.mixin.android.db.web3.vo.Web3TransactionItem
import one.mixin.android.extension.loadImage
import one.mixin.android.widget.BadgeCircleImageView

internal sealed interface Web3TransactionHeaderIcon {
    data class Token(
        val iconUrl: String?,
        val chainIconUrl: String?,
    ) : Web3TransactionHeaderIcon

    data class Drawable(val resId: Int) : Web3TransactionHeaderIcon
}

internal fun Web3TransactionItem.resolveHeaderIcon(): Web3TransactionHeaderIcon =
    when {
        status == TransactionStatus.NOT_FOUND.value ||
            status == TransactionStatus.FAILED.value ||
            status == TransactionStatus.PENDING.value -> {
            Web3TransactionHeaderIcon.Drawable(R.drawable.ic_web3_transaction_contract)
        }

        transactionType == TransactionType.TRANSFER_OUT.value && senders.size > 1 -> {
            Web3TransactionHeaderIcon.Drawable(R.drawable.ic_snapshot_withdrawal)
        }

        transactionType == TransactionType.TRANSFER_OUT.value -> {
            Web3TransactionHeaderIcon.Token(sendAssetIconUrl, chainIconUrl)
        }

        transactionType == TransactionType.TRANSFER_IN.value && receivers.size > 1 -> {
            Web3TransactionHeaderIcon.Drawable(R.drawable.ic_snapshot_deposit)
        }

        transactionType == TransactionType.TRANSFER_IN.value -> {
            Web3TransactionHeaderIcon.Token(receiveAssetIconUrl, chainIconUrl)
        }

        transactionType == TransactionType.SWAP.value -> {
            Web3TransactionHeaderIcon.Drawable(R.drawable.ic_web3_transaction_swap)
        }

        transactionType == TransactionType.APPROVAL.value -> {
            Web3TransactionHeaderIcon.Drawable(R.drawable.ic_web3_transaction_approval)
        }

        else -> {
            Web3TransactionHeaderIcon.Drawable(R.drawable.ic_web3_transaction_unknown)
        }
    }

internal fun BadgeCircleImageView.bindTransactionHeaderIcon(transaction: Web3TransactionItem) {
    when (val icon = transaction.resolveHeaderIcon()) {
        is Web3TransactionHeaderIcon.Token -> {
            bg.loadImage(icon.iconUrl, R.drawable.ic_avatar_place_holder)
            badge.isVisible = true
            badge.loadImage(icon.chainIconUrl, R.drawable.ic_avatar_place_holder)
        }

        is Web3TransactionHeaderIcon.Drawable -> {
            bg.setImageResource(icon.resId)
            badge.isVisible = false
        }
    }
}
