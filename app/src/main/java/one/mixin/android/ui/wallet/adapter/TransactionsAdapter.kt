package one.mixin.android.ui.wallet.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_wallet_transactions.view.*
import one.mixin.android.R
import one.mixin.android.extension.date
import one.mixin.android.extension.formatPublicKey
import one.mixin.android.extension.numberFormat
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.SnapshotType
import org.jetbrains.anko.textColorResource

class TransactionsAdapter(var snapshots: List<SnapshotItem>, var asset: AssetItem)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_NORMAL = 1
    }

    var header: View? = null
    private var transactionsListener: TransactionsListener? = null

    override fun getItemCount(): Int = if (header != null) snapshots.size + 1 else snapshots.size

    override fun getItemViewType(position: Int): Int {
        return if (position == TYPE_HEADER && header != null) {
            TYPE_HEADER
        } else {
            TYPE_NORMAL
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is NormalHolder) {
            val snapshot = snapshots[getPos(position)]
            holder.bind(snapshot, asset, transactionsListener)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == TYPE_HEADER) {
            return HeadHolder(header!!)
        }
        return NormalHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wallet_transactions, parent, false))
    }

    private fun getPos(position: Int): Int {
        return if (header != null) {
            position - 1
        } else {
            position
        }
    }

    class NormalHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(snapshot: SnapshotItem, asset: AssetItem, listener: TransactionsListener?) {
            val isPositive = snapshot.amount.toFloat() > 0
            itemView.date.text = snapshot.createdAt.date()
            when {
                snapshot.type == SnapshotType.deposit.name -> {
                    if (asset.accountName != null) {
                        itemView.name.text = itemView.context.getString(R.string.transaction_item_deposit, asset.accountName, asset.accountMemo)
                    } else {
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

            itemView.setOnClickListener { listener?.onItemClick(snapshot) }
        }
    }

    class HeadHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    fun setListener(listener: TransactionsListener) {
        this.transactionsListener = listener
    }

    interface TransactionsListener {
        fun onItemClick(snapshot: SnapshotItem)
    }
}