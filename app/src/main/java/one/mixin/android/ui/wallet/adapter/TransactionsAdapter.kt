package one.mixin.android.ui.wallet.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import kotlinx.android.synthetic.main.item_transaction_header.view.*
import kotlinx.android.synthetic.main.item_wallet_transactions.view.*
import one.mixin.android.R
import one.mixin.android.extension.formatPublicKey
import one.mixin.android.extension.hashForDate
import one.mixin.android.extension.inflate
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.timeAgoDate
import one.mixin.android.ui.common.recyclerview.HeaderAdapter
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.SnapshotType
import org.jetbrains.anko.backgroundResource
import org.jetbrains.anko.textColorResource

class TransactionsAdapter(var asset: AssetItem) : HeaderAdapter<SnapshotItem>(), StickyRecyclerHeadersAdapter<TransactionsAdapter.TransactionHeaderViewHolder> {

    var listener: OnTransactionsListener? = null

    override fun getHeaderId(pos: Int): Long {
        return if (headerView != null && pos == TYPE_HEADER) {
            -1
        } else {
            val snapshot = data!![getPos(pos)]
            Math.abs(snapshot.createdAt.hashForDate())
        }
    }

    override fun onCreateHeaderViewHolder(parent: ViewGroup) =
        TransactionHeaderViewHolder(parent.inflate(R.layout.item_transaction_header, false))

    override fun onBindHeaderViewHolder(vh: TransactionHeaderViewHolder, pos: Int) {
        val time = data!![getPos(pos)].createdAt
        vh.bind(time)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is TransactionsHolder) {
            val isLast = position == itemCount - 1
            val pos = getPos(position)
            val snapshot = data!![pos]
            holder.bind(snapshot, asset, listener, isLast)
        }
    }

    override fun getNormalViewHolder(context: Context, parent: ViewGroup): NormalHolder =
        TransactionsHolder(LayoutInflater.from(context).inflate(R.layout.item_wallet_transactions, parent, false))

    class TransactionsHolder(itemView: View) : HeaderAdapter.NormalHolder(itemView) {
        fun bind(snapshot: SnapshotItem, asset: AssetItem, listener: OnTransactionsListener?, isLast: Boolean) {
            val isPositive = snapshot.amount.toFloat() > 0
            when {
                snapshot.type == SnapshotType.pending.name -> {
                    itemView.name.text = itemView.context.getString(R.string.pending_confirmations, snapshot.confirmations, asset.confirmations)
                    itemView.avatar.setUrl(null, R.drawable.ic_transaction_up)
                    itemView.bg.setConfirmation(asset.confirmations, snapshot.confirmations ?: 0)
                }
                snapshot.type == SnapshotType.deposit.name -> {
                    itemView.name.setText(R.string.filters_deposit)
                    itemView.avatar.setUrl(null, R.drawable.ic_transaction_down)
                }
                snapshot.type == SnapshotType.transfer.name -> {
                    itemView.name.setText(R.string.filters_transfer)
                    itemView.avatar.setInfo(snapshot.opponentFullName, snapshot.avatarUrl, snapshot.opponentId ?: "")
                    itemView.avatar.setOnClickListener {
                        listener?.onUserClick(snapshot.opponentId!!)
                    }
                    itemView.avatar.setTextSize(12f)
                }
                snapshot.type == SnapshotType.withdrawal.name -> {
                    itemView.name.setText(R.string.filters_withdrawal)
                    itemView.avatar.setUrl(null, R.drawable.ic_transaction_up)
                }
                snapshot.type == SnapshotType.fee.name -> {
                    itemView.name.setText(R.string.filters_fee)
                    itemView.avatar.setUrl(null, R.drawable.ic_transaction_up)
                }
                snapshot.type == SnapshotType.rebate.name -> {
                    itemView.name.setText(R.string.filters_rebate)
                    itemView.avatar.setUrl(null, R.drawable.ic_transaction_down)
                }
                else -> {
                    itemView.name.text = snapshot.receiver!!.formatPublicKey()
                    itemView.avatar.setUrl(null, R.drawable.ic_transaction_down)
                }
            }

            if (isLast) {
                itemView.root.backgroundResource = R.drawable.bg_wallet_transactions_bottom
                itemView.bg.roundBottom(true)
                itemView.transaction_shadow_left.visibility = GONE
                itemView.transaction_shadow_right.visibility = GONE
            } else {
                itemView.root.backgroundResource = R.color.white
                itemView.bg.roundBottom(false)
                itemView.transaction_shadow_left.visibility = VISIBLE
                itemView.transaction_shadow_right.visibility = VISIBLE
            }

            itemView.value.text = if (isPositive) "+${snapshot.amount.numberFormat()}"
            else snapshot.amount.numberFormat()
            itemView.value.textColorResource = when {
                snapshot.type == SnapshotType.pending.name -> R.color.wallet_text_dark
                isPositive -> R.color.wallet_green
                else -> R.color.wallet_pink
            }

            itemView.setOnClickListener {
                if (snapshot.type != SnapshotType.pending.name) {
                    listener?.onNormalItemClick(snapshot)
                }
            }
        }
    }

    class TransactionHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(time: String) {
            itemView.date_tv.timeAgoDate(time)
        }
    }

    interface OnTransactionsListener {
        fun <T> onNormalItemClick(item: T)
        fun onUserClick(userId: String)
    }
}