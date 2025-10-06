package one.mixin.android.ui.wallet

import android.content.ClipData
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import one.mixin.android.R
import one.mixin.android.databinding.ItemWeb3TransactionsBinding
import one.mixin.android.db.web3.vo.Web3TransactionItem
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.hashForDate
import one.mixin.android.ui.wallet.adapter.OnSnapshotListener
import one.mixin.android.ui.wallet.adapter.SnapshotHeaderViewHolder
import one.mixin.android.util.debug.debugLongClick
import one.mixin.android.web3.details.Web3TransactionHolder
import kotlin.math.abs

class Web3SnapshotLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    var list = emptyList<Web3TransactionItem>()
        set(value) {
            field = value
            updateViews()
        }

    var listener: OnSnapshotListener? = null

    init {
        orientation = VERTICAL
    }

    private fun updateViews() {
        removeAllViews()
        var currentId: Long? = null
        list.take(20).forEach { item ->
            val id = getHeaderId(item)
            if (id != currentId) {
                currentId = id
                val headerView = LayoutInflater.from(context)
                    .inflate(R.layout.item_transaction_header, this, false)
                val headerHolder = SnapshotHeaderViewHolder(headerView, true)
                headerHolder.bind(item.transactionAt)
                addView(headerView)
            }

            val itemBinding = ItemWeb3TransactionsBinding.inflate(LayoutInflater.from(context), this, false)
            val holder = Web3TransactionHolder(itemBinding)
            holder.bind(item)
            holder.itemView.setOnClickListener {
                listener?.onNormalItemClick(item)
            }
            debugLongClick(
                itemBinding.root,
                {
                    context.getClipboardManager()
                        .setPrimaryClip(ClipData.newPlainText(null, item.transactionHash))
                },
            )
            addView(itemBinding.root)
        }
        if (list.size > 20) {
            val itemView = LayoutInflater.from(context)
                .inflate(R.layout.item_wallet_transactions_more, this, false)
            itemView.setOnClickListener {
                listener?.onMoreClick()
            }
            addView(itemView)
        }
    }

    fun getHeaderId(snapshot: Web3TransactionItem): Long {
        return abs(snapshot.transactionAt.hashForDate())
    }
}
