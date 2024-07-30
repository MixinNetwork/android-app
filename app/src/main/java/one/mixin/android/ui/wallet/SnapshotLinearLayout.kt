package one.mixin.android.ui.wallet

import android.content.ClipData
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import one.mixin.android.R
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.hashForDate
import one.mixin.android.ui.wallet.adapter.OnSnapshotListener
import one.mixin.android.ui.wallet.adapter.SnapshotHeaderViewHolder
import one.mixin.android.ui.wallet.adapter.SnapshotHolder
import one.mixin.android.util.debug.debugLongClick
import one.mixin.android.vo.SnapshotItem
import kotlin.math.abs

class SnapshotLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    var list = emptyList<SnapshotItem>()
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
        list.take(20).forEach{ item ->
            val id = getHeaderId(item)
            if (id != currentId) {
                currentId = id
                val headerView = LayoutInflater.from(context)
                    .inflate(R.layout.item_transaction_header, this, false)
                val headerHolder = SnapshotHeaderViewHolder(headerView)
                headerHolder.bind(item.createdAt)
                addView(headerView)
            }

            val itemView = LayoutInflater.from(context)
                .inflate(R.layout.item_wallet_transactions, this, false)
            val holder = SnapshotHolder(itemView)
            holder.bind(item, listener)
            debugLongClick(
                itemView,
                {
                    context.getClipboardManager().setPrimaryClip(ClipData.newPlainText(null, item.snapshotId))
                },
            )
            addView(itemView)
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

    fun getHeaderId(snapshot: SnapshotItem): Long {
        return abs(snapshot.createdAt.hashForDate())
    }
}
