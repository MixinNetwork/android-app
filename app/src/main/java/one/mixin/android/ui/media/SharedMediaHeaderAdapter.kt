package one.mixin.android.ui.media

import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import one.mixin.android.R
import one.mixin.android.databinding.ItemSharedMediaHeaderBinding
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.hashForDate
import one.mixin.android.extension.inflate
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.vo.MessageItem
import kotlin.math.abs

abstract class SharedMediaHeaderAdapter<VH : NormalHolder>(diffCallback: DiffUtil.ItemCallback<MessageItem> = MessageItem.DIFF_CALLBACK) :
    PagedListAdapter<MessageItem, VH>(diffCallback),
    StickyRecyclerHeadersAdapter<MediaHeaderViewHolder> {

    override fun getHeaderId(pos: Int): Long {
        val messageItem = getItem(pos)
        return abs(messageItem?.createdAt?.hashForDate() ?: -1)
    }

    override fun onCreateHeaderViewHolder(parent: ViewGroup): MediaHeaderViewHolder {
        val view = parent.inflate(R.layout.item_shared_media_header, false)
        val binding = ItemSharedMediaHeaderBinding.bind(view)
        binding.dateTv.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            val margin = parent.context.dpToPx(getHeaderTextMargin())
            marginStart = margin
            marginEnd = margin
        }
        return MediaHeaderViewHolder(view)
    }

    override fun onBindHeaderViewHolder(holder: MediaHeaderViewHolder, pos: Int) {
        val time = getItem(pos)?.createdAt ?: return
        holder.bind(time)
    }

    abstract fun getHeaderTextMargin(): Float
}
