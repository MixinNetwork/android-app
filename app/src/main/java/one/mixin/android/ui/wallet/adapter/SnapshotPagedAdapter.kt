package one.mixin.android.ui.wallet.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import one.mixin.android.R
import one.mixin.android.extension.hashForDate
import one.mixin.android.extension.inflate
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.SnapshotItem.Companion.DIFF_CALLBACK
import kotlin.math.abs

class SnapshotPagedAdapter :
    PagedListAdapter<SnapshotItem, SnapshotHolder>(DIFF_CALLBACK),
    StickyRecyclerHeadersAdapter<SnapshotHeaderViewHolder> {

    var listener: OnSnapshotListener? = null

    override fun getHeaderId(pos: Int): Long {
        val snapshot = getItem(pos)
        return if (snapshot == null) {
            -1
        } else {
            abs(snapshot.createdAt.hashForDate())
        }
    }

    override fun onCreateHeaderViewHolder(parent: ViewGroup) =
        SnapshotHeaderViewHolder(parent.inflate(R.layout.item_transaction_header, false))

    override fun onBindHeaderViewHolder(vh: SnapshotHeaderViewHolder, pos: Int) {
        getItem(pos)?.let {
            vh.bind(it.createdAt)
        }
    }

    override fun onBindViewHolder(holder: SnapshotHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it, listener)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SnapshotHolder {
        return SnapshotHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_wallet_transactions, parent, false)
        )
    }
}
