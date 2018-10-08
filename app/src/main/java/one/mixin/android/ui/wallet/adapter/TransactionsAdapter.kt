package one.mixin.android.ui.wallet.adapter

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_wallet_transactions.view.*
import one.mixin.android.R
import one.mixin.android.extension.date
import one.mixin.android.extension.formatPublicKey
import one.mixin.android.extension.numberFormat
import one.mixin.android.ui.common.headrecyclerview.HeaderAdapter
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.SnapshotType
import org.jetbrains.anko.textColorResource

class TransactionsAdapter(var asset: AssetItem) : HeaderAdapter<SnapshotItem>() {

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is TransactionsHolder) {
            val snapshot = data!![getPos(position)]
            holder.bind(snapshot, asset, onItemListener)
        }
    }

    override fun getNormalViewHolder(context: Context, parent: ViewGroup): NormalHolder =
        TransactionsHolder(LayoutInflater.from(context).inflate(R.layout.item_wallet_transactions, parent, false))

    class TransactionsHolder(itemView: View) : HeaderAdapter.NormalHolder(itemView) {
        fun bind(snapshot: SnapshotItem, asset: AssetItem, listener: OnItemListener?) {
            val isPositive = snapshot.amount.toFloat() > 0
            itemView.date.text = snapshot.createdAt.date()
            when {
                snapshot.type == SnapshotType.deposit.name -> {
                    snapshot.transactionHash?.let {
                        if (it.length > 10) {
                            val start = it.substring(0, 6)
                            val end = it.substring(it.length - 4, it.length)
                            itemView.name.text = itemView.context.getString(R.string.wallet_transactions_hash, start, end)
                        } else {
                            itemView.name.text = it
                        }
                    }
                }
                snapshot.type == SnapshotType.transfer.name -> itemView.name.text = if (isPositive) {
                    itemView.context.getString(R.string.transfer_from, snapshot.opponentFullName)
                } else {
                    itemView.context.getString(R.string.transfer_to, snapshot.opponentFullName)
                }
                else -> itemView.name.text = snapshot.receiver!!.formatPublicKey()
            }
            itemView.value.text = if (isPositive) "+${snapshot.amount.numberFormat()} ${asset.symbol}"
            else "${snapshot.amount.numberFormat()} ${asset.symbol}"
            itemView.value.textColorResource = if (isPositive) R.color.colorGreen else R.color.colorRed

            itemView.setOnClickListener { listener?.onNormalItemClick(snapshot) }
        }
    }
}