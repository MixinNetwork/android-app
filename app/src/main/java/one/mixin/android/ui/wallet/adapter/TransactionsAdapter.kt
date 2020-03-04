package one.mixin.android.ui.wallet.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import kotlin.math.abs
import one.mixin.android.R
import one.mixin.android.extension.hashForDate
import one.mixin.android.extension.inflate
import one.mixin.android.ui.common.recyclerview.PagedHeaderAdapter
import one.mixin.android.vo.SnapshotItem

class TransactionsAdapter :
    PagedHeaderAdapter<SnapshotItem>(SnapshotItem.DIFF_CALLBACK),
    StickyRecyclerHeadersAdapter<SnapshotHeaderViewHolder> {
    var listener: OnSnapshotListener? = null

    override fun getHeaderId(pos: Int): Long {
        return if (headerView != null && pos == TYPE_HEADER) {
            -1
        } else {
            val snapshot = getItem(getPos(pos))
            abs(snapshot?.createdAt?.hashForDate() ?: -1)
        }
    }

    override fun onCreateHeaderViewHolder(parent: ViewGroup) =
        SnapshotHeaderViewHolder(parent.inflate(R.layout.item_transaction_header, false))

    override fun onBindHeaderViewHolder(vh: SnapshotHeaderViewHolder, pos: Int) {
        val time = getItem(getPos(pos))?.createdAt ?: return
        vh.bind(time)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is SnapshotHolder) {
            val pos = getPos(position)
            getItem(pos)?.let {
                holder.bind(it, listener)
            }
        }
    }

    override fun getNormalViewHolder(context: Context, parent: ViewGroup) =
        SnapshotHolder(
            LayoutInflater.from(context).inflate(
                R.layout.item_wallet_transactions,
                parent,
                false
            )
        )
}
