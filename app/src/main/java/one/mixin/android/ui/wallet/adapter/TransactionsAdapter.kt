package one.mixin.android.ui.wallet.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
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
                snapshot.type == SnapshotType.pending.name -> {
                    itemView.state.visibility = View.VISIBLE
                    itemView.name.text = itemView.context.getString(R.string.pending_confirmations, snapshot.confirmations, asset.confirmations)
                }
                snapshot.type == SnapshotType.deposit.name -> {
                    itemView.state.visibility = View.GONE
                    itemView.name.setText(R.string.filters_deposit)
                }
                snapshot.type == SnapshotType.transfer.name -> itemView.name.text = if (isPositive) {
                    itemView.state.visibility = View.GONE
                    itemView.context.getString(R.string.transfer_from, snapshot.opponentFullName)
                } else {
                    itemView.state.visibility = View.GONE
                    itemView.context.getString(R.string.transfer_to, snapshot.opponentFullName)
                }
                snapshot.type == SnapshotType.withdrawal.name -> {
                    itemView.state.visibility = View.GONE
                    itemView.name.setText(R.string.filters_withdrawal)
                }
                snapshot.type == SnapshotType.fee.name -> {
                    itemView.state.visibility = View.GONE
                    itemView.name.setText(R.string.filters_fee)
                }
                snapshot.type == SnapshotType.rebate.name -> {
                    itemView.state.visibility = View.GONE
                    itemView.name.setText(R.string.filters_rebate)
                }
                else -> {
                    itemView.state.visibility = View.GONE
                    itemView.name.text = snapshot.receiver!!.formatPublicKey()
                }
            }

            itemView.value.text = if (isPositive) "+${snapshot.amount.numberFormat()} ${asset.symbol}"
            else "${snapshot.amount.numberFormat()} ${asset.symbol}"
            itemView.value.textColorResource = when {
                snapshot.type == SnapshotType.pending.name -> R.color.text_gray
                isPositive -> R.color.colorGreen
                else -> R.color.colorRed
            }

            itemView.setOnClickListener {
                if (snapshot.type != SnapshotType.pending.name) {
                    listener?.onNormalItemClick(snapshot)
                }
            }
        }
    }
}