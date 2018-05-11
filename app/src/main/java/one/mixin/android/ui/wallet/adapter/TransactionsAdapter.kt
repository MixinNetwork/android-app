package one.mixin.android.ui.wallet.adapter

import android.arch.paging.PagedListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_wallet_transactions.view.*
import one.mixin.android.R
import one.mixin.android.extension.date
import one.mixin.android.extension.formatPublicKey
import one.mixin.android.extension.numberFormat
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.SnapshotType
import org.jetbrains.anko.textColorResource

class TransactionsAdapter : PagedListAdapter<SnapshotItem, RecyclerView.ViewHolder>(diffCallback) {
    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_NORMAL = 1

        private val diffCallback = object : DiffUtil.ItemCallback<SnapshotItem>() {
            override fun areItemsTheSame(oldItem: SnapshotItem, newItem: SnapshotItem): Boolean {
                return oldItem.snapshotId == newItem.snapshotId
            }

            override fun areContentsTheSame(oldItem: SnapshotItem, newItem: SnapshotItem): Boolean {
                return oldItem == newItem
            }
        }
    }

    var header: View? = null
    private var transactionsListener: TransactionsListener? = null

    override fun getItemCount(): Int = if (header != null) super.getItemCount() + 1 else super.getItemCount()

    override fun getItemViewType(position: Int): Int {
        return if (position == TYPE_HEADER && header != null) {
            TYPE_HEADER
        } else {
            TYPE_NORMAL
        }
    }

    override fun getItem(position: Int): SnapshotItem? {
        return if (header != null) {
            if (getItemViewType(position) == TYPE_HEADER) {
                return null
            }
            return super.getItem(position - 1)
        } else {
            super.getItem(position)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        getItem(position)?.let {
            if (holder is NormalHolder) {
                holder.bind(it, transactionsListener)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == TYPE_HEADER) {
            return HeadHolder(header!!)
        }
        return NormalHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wallet_transactions, parent, false))
    }

    class NormalHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(snapshot: SnapshotItem, listener: TransactionsListener?) {
            val isPositive = snapshot.amount.toFloat() > 0
            itemView.date.text = snapshot.createdAt.date()
            when {
                snapshot.type == SnapshotType.deposit.name -> snapshot.transactionHash?.let {
                    if (it.length > 10) {
                        val start = it.substring(0, 6)
                        val end = it.substring(it.length - 4, it.length)
                        itemView.name.text = itemView.context.getString(R.string.wallet_transactions_hash, start, end)
                    } else {
                        itemView.name.text = it
                    }
                }
                snapshot.type == SnapshotType.transfer.name -> itemView.name.text = if (isPositive) {
                    itemView.context.getString(R.string.transfer_from, snapshot.counterFullName)
                } else {
                    itemView.context.getString(R.string.transfer_to, snapshot.counterFullName)
                }
                else -> itemView.name.text = snapshot.receiver!!.formatPublicKey()
            }
            itemView.value.text = if (isPositive) "+${snapshot.amount.numberFormat()} ${snapshot.assetSymbol}" else "${snapshot.amount.numberFormat()} ${snapshot.assetSymbol}"
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