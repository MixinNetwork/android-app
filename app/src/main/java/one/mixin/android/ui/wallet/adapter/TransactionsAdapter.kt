package one.mixin.android.ui.wallet.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import one.mixin.android.R
import one.mixin.android.extension.hashForDate
import one.mixin.android.extension.inflate
import one.mixin.android.ui.common.recyclerview.HeaderAdapter
import one.mixin.android.vo.SnapshotItem

class TransactionsAdapter : HeaderAdapter<SnapshotItem>(), StickyRecyclerHeadersAdapter<SnapshotHeaderViewHolder> {

    var listener: OnSnapshotListener? = null

    override fun getHeaderId(pos: Int): Long {
        return if (headerView != null && pos == TYPE_HEADER) {
            -1
        } else {
            val snapshot = data!![getPos(pos)]
            Math.abs(snapshot.createdAt.hashForDate())
        }
    }

    override fun onCreateHeaderViewHolder(parent: ViewGroup) =
        SnapshotHeaderViewHolder(parent.inflate(R.layout.item_transaction_header, false))

    override fun onBindHeaderViewHolder(vh: SnapshotHeaderViewHolder, pos: Int) {
        val time = data!![getPos(pos)].createdAt
        vh.bind(time)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is SnapshotHolder) {
            val isLast = position == itemCount - 1
            val pos = getPos(position)
            data!![pos].let {
                holder.bind(it, listener, isLast)
            }
        }
    }

    override fun getNormalViewHolder(context: Context, parent: ViewGroup) =
        SnapshotHolder(LayoutInflater.from(context).inflate(R.layout.item_wallet_transactions, parent, false))
}