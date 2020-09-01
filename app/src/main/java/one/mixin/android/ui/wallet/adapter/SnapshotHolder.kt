package one.mixin.android.ui.wallet.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_transaction_header.view.*
import kotlinx.android.synthetic.main.item_wallet_transactions.view.*
import one.mixin.android.R
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.timeAgoDay
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.SnapshotType
import org.jetbrains.anko.textColorResource

open class SnapshotHolder(itemView: View) : NormalHolder(itemView) {

    open fun bind(snapshot: SnapshotItem, listener: OnSnapshotListener?) {
        val isPositive = snapshot.amount.toFloat() > 0
        when (snapshot.type) {
            SnapshotType.pending.name -> {
                itemView.name.text = itemView.context.getString(R.string.pending_confirmations, snapshot.confirmations, snapshot.assetConfirmations)
                itemView.avatar.setNet()
                itemView.bg.setConfirmation(snapshot.assetConfirmations, snapshot.confirmations ?: 0)
            }
            SnapshotType.deposit.name -> {
                itemView.name.setText(R.string.filters_deposit)
                itemView.avatar.setNet()
            }
            SnapshotType.transfer.name -> {
                itemView.name.setText(R.string.filters_transfer)
                itemView.avatar.setInfo(snapshot.opponentFullName, snapshot.avatarUrl, snapshot.opponentId ?: "")
                itemView.avatar.setOnClickListener {
                    listener?.onUserClick(snapshot.opponentId!!)
                }
                itemView.avatar.setTextSize(12f)
            }
            SnapshotType.withdrawal.name -> {
                itemView.name.setText(R.string.filters_withdrawal)
                itemView.avatar.setNet()
            }
            SnapshotType.fee.name -> {
                itemView.name.setText(R.string.filters_fee)
                itemView.avatar.setNet()
            }
            SnapshotType.rebate.name -> {
                itemView.name.setText(R.string.filters_rebate)
                itemView.avatar.setNet()
            }
            SnapshotType.raw.name -> {
                itemView.name.setText(R.string.filters_raw)
                itemView.avatar.setNet()
            }
            else -> {
                itemView.name.text = snapshot.type
                itemView.avatar.setNet()
            }
        }

        itemView.value.text = if (isPositive) "+${snapshot.amount.numberFormat()}"
        else snapshot.amount.numberFormat()
        itemView.value.textColorResource = when {
            snapshot.type == SnapshotType.pending.name -> R.color.wallet_pending_text_color
            isPositive -> R.color.wallet_green
            else -> R.color.wallet_pink
        }
        itemView.symbol_tv.text = snapshot.assetSymbol

        itemView.setOnClickListener {
            listener?.onNormalItemClick(snapshot)
        }
    }
}

class SnapshotHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bind(time: String) {
        itemView.date_tv.timeAgoDay(time)
    }
}

interface OnSnapshotListener {
    fun <T> onNormalItemClick(item: T)
    fun onUserClick(userId: String)
}
