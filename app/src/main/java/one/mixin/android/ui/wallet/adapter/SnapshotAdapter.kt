package one.mixin.android.ui.wallet.adapter

import android.annotation.SuppressLint
import android.content.ClipData
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import one.mixin.android.R
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.hashForDate
import one.mixin.android.extension.inflate
import one.mixin.android.util.debug.debugLongClick
import one.mixin.android.vo.SnapshotItem
import kotlin.math.abs

class SnapshotAdapter : RecyclerView.Adapter<SnapshotHolder>(),
    StickyRecyclerHeadersAdapter<SnapshotHeaderViewHolder> {

    var list = emptyList<SnapshotItem>()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    fun getItem(pos: Int) = list[pos]

    var listener: OnSnapshotListener? = null

    override fun getHeaderId(pos: Int): Long {
        val snapshot = getItem(pos)
        return abs(snapshot.createdAt.hashForDate() ?: -1)
    }

    override fun onCreateHeaderViewHolder(parent: ViewGroup) =
        SnapshotHeaderViewHolder(parent.inflate(R.layout.item_transaction_header, false))

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindHeaderViewHolder(
        vh: SnapshotHeaderViewHolder,
        pos: Int,
    ) {
        val time = getItem(pos)?.createdAt ?: return
        vh.bind(time)
    }

    override fun onBindViewHolder(
        holder: SnapshotHolder,
        position: Int,
    ) {
        getItem(position)?.let {
            holder.bind(it, listener)
            debugLongClick(
                holder.itemView,
                {
                    holder.itemView.context?.getClipboardManager()
                        ?.setPrimaryClip(ClipData.newPlainText(null, it.snapshotId))
                },
            )
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): SnapshotHolder {
        return SnapshotHolder(parent.inflate(R.layout.item_wallet_transactions, false))
    }

}
