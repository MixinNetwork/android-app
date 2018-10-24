package one.mixin.android.ui.wallet.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_wallet_transactions.view.*
import one.mixin.android.R
import one.mixin.android.extension.date
import one.mixin.android.extension.formatPublicKey
import one.mixin.android.extension.numberFormat
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.SnapshotType
import org.jetbrains.anko.textColorResource

class SnapshotAdapter : PagedListAdapter<SnapshotItem, SnapshotAdapter.NormalHolder>(
    object : DiffUtil.ItemCallback<SnapshotItem>() {
        override fun areItemsTheSame(oldItem: SnapshotItem, newItem: SnapshotItem): Boolean {
            return oldItem.snapshotId == newItem.snapshotId
        }

        override fun areContentsTheSame(oldItem: SnapshotItem, newItem: SnapshotItem): Boolean {
            return oldItem == newItem
        }
    }) {
    private var transactionsListener: TransactionsListener? = null

    override fun onBindViewHolder(holder: NormalHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it, transactionsListener)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NormalHolder {
        return NormalHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wallet_transactions, parent, false))
    }

    class NormalHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(snapshot: SnapshotItem, listener: TransactionsListener?) {
            val isPositive = snapshot.amount.toFloat() > 0
            itemView.date.text = snapshot.createdAt.date()
            when {
                snapshot.type == SnapshotType.deposit.name ->
                    itemView.name.setText(R.string.filters_deposit)
                snapshot.type == SnapshotType.transfer.name -> itemView.name.text = if (isPositive) {
                    itemView.context.getString(R.string.transfer_from, snapshot.opponentFullName)
                } else {
                    itemView.context.getString(R.string.transfer_to, snapshot.opponentFullName)
                }
                snapshot.type == SnapshotType.withdrawal.name -> {
                    itemView.name.setText(R.string.filters_withdrawal)
                }
                snapshot.type == SnapshotType.fee.name -> {
                    itemView.name.setText(R.string.filters_fee)
                }
                snapshot.type == SnapshotType.rebate.name -> {
                    itemView.name.setText(R.string.filters_rebate)
                }
                else -> itemView.name.text = snapshot.receiver!!.formatPublicKey()
            }
            itemView.value.text = if (isPositive) "+${snapshot.amount.numberFormat()} ${snapshot.assetSymbol}"
            else "${snapshot.amount.numberFormat()} ${snapshot.assetSymbol}"
            itemView.value.textColorResource = if (isPositive) R.color.colorGreen else R.color.colorRed

            itemView.setOnClickListener { listener?.onItemClick(snapshot) }
        }
    }

    fun setListener(listener: TransactionsListener) {
        this.transactionsListener = listener
    }

    interface TransactionsListener {
        fun onItemClick(snapshot: SnapshotItem)
    }
}