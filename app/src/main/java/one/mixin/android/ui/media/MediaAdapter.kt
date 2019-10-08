package one.mixin.android.ui.media

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import kotlinx.android.synthetic.main.item_transaction_header.view.*
import one.mixin.android.R
import one.mixin.android.extension.hashForDate
import one.mixin.android.extension.inflate
import one.mixin.android.extension.timeAgoDay
import one.mixin.android.ui.common.recyclerview.PagedHeaderAdapter
import one.mixin.android.vo.MessageItem
import kotlin.math.abs

class MediaAdapter(private val onClickListener: (imageView: View, messageId: String) -> Unit) :
    PagedHeaderAdapter<MessageItem, MediaHolder>(MessageItem.DIFF_CALLBACK),
    StickyRecyclerHeadersAdapter<MediaHeaderViewHolder> {

    var size: Int = 0

    override fun getNormalViewHolder(context: Context, parent: ViewGroup) =
        MediaHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_media,
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: MediaHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it, size, onClickListener)
        }
    }

    override fun getHeaderId(pos: Int): Long {
        return if (headerView != null && pos == TYPE_HEADER) {
            -1
        } else {
            val snapshot = getItem(getPos(pos))
            abs(snapshot?.createdAt?.hashForDate() ?: -1)
        }
    }

    override fun onCreateHeaderViewHolder(parent: ViewGroup) =
        MediaHeaderViewHolder(parent.inflate(R.layout.item_transaction_header, false))

    override fun onBindHeaderViewHolder(holder: MediaHeaderViewHolder, pos: Int) {
        val time = getItem(getPos(pos))?.createdAt ?: return
        holder.bind(time)
    }
}

class MediaHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bind(time: String) {
        itemView.date_tv.timeAgoDay(time)
    }
}