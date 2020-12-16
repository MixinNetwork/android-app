package one.mixin.android.ui.wallet.adapter

import android.content.ClipData
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import one.mixin.android.R
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.hashForDate
import one.mixin.android.extension.inflate
import one.mixin.android.ui.common.recyclerview.PagedHeaderAdapter
import one.mixin.android.util.debug.debugLongClick
import one.mixin.android.vo.SnapshotItem
import kotlin.math.abs

class TransactionsAdapter :
    PagedHeaderAdapter<SnapshotItem>(SnapshotItem.DIFF_CALLBACK),
    StickyRecyclerHeadersAdapter<SnapshotHeaderViewHolder> {
    var listener: OnSnapshotListener? = null

    override fun getHeaderId(pos: Int): Long {
        return if (isShowHeader() && pos == TYPE_HEADER) {
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
                debugLongClick(holder.itemView) {
                    holder.itemView.context?.getClipboardManager()
                        ?.setPrimaryClip(ClipData.newPlainText(null, it.snapshotId))
                }
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
