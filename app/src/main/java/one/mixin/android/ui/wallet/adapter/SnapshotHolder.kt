package one.mixin.android.ui.wallet.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemTransactionHeaderBinding
import one.mixin.android.databinding.ItemWalletTransactionsBinding
import one.mixin.android.extension.formatPublicKey
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.textColorResource
import one.mixin.android.extension.timeAgoDay
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.safe.SafeSnapshotType

open class SnapshotHolder(itemView: View) : NormalHolder(itemView) {

    private val binding = ItemWalletTransactionsBinding.bind(itemView)

    open fun bind(snapshot: SnapshotItem, listener: OnSnapshotListener?) {
        val isPositive = snapshot.amount.toFloat() > 0
        when (val type = snapshot.simulateType()) {
            SafeSnapshotType.transfer -> {
                binding.name.text = if (snapshot.opponentId.isBlank()) {
                    "N/A"
                } else snapshot.opponentFullName
                binding.avatar.setInfo(snapshot.opponentFullName, snapshot.avatarUrl, snapshot.opponentId)
                binding.avatar.setOnClickListener {
                    listener?.onUserClick(snapshot.opponentId)
                }
            }
            SafeSnapshotType.pending -> {
                binding.name.text = itemView.context.resources.getQuantityString(R.plurals.pending_confirmation, snapshot.confirmations ?: 0, snapshot.confirmations ?: 0, snapshot.assetConfirmations)
                binding.avatar.setNet()
                binding.bg.setConfirmation(snapshot.assetConfirmations, snapshot.confirmations ?: 0)
            }
            else -> {
                if (type == SafeSnapshotType.deposit) {
                    binding.avatar.setDeposit()
                } else {
                    binding.avatar.setWithdrawal()
                }
                binding.name.text = snapshot.transactionHash?.formatPublicKey()
            }
        }

        binding.value.text = if (isPositive) {
            "+${snapshot.amount.numberFormat()}"
        } else {
            snapshot.amount.numberFormat()
        }
        binding.value.textColorResource = when {
            snapshot.type == SafeSnapshotType.pending.name -> R.color.wallet_pending_text_color
            isPositive -> R.color.wallet_green
            else -> R.color.wallet_pink
        }
        binding.symbolTv.text = snapshot.assetSymbol

        itemView.setOnClickListener {
            listener?.onNormalItemClick(snapshot)
        }
    }
}

class SnapshotHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val binding = ItemTransactionHeaderBinding.bind(itemView)
    fun bind(time: String) {
        binding.dateTv.timeAgoDay(time)
    }
}

interface OnSnapshotListener {
    fun <T> onNormalItemClick(item: T)
    fun onUserClick(userId: String)
}
