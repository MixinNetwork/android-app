package one.mixin.android.ui.media

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.paging.PagedListAdapter
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import kotlin.math.abs
import kotlinx.android.synthetic.main.item_shared_media_header.view.*
import kotlinx.android.synthetic.main.item_shared_media_link.view.*
import one.mixin.android.R
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.hashForDate
import one.mixin.android.extension.inflate
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.vo.HyperlinkItem

class LinkAdapter(private val onClickListener: (url: String) -> Unit) :
    PagedListAdapter<HyperlinkItem, LinkHolder>(HyperlinkItem.DIFF_CALLBACK),
    StickyRecyclerHeadersAdapter<MediaHeaderViewHolder> {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        LinkHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_shared_media_link,
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: LinkHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it, onClickListener)
        }
    }

    override fun getHeaderId(pos: Int): Long {
        val messageItem = getItem(pos)
        return abs(messageItem?.createdAt?.hashForDate() ?: -1)
    }

    override fun onCreateHeaderViewHolder(parent: ViewGroup): MediaHeaderViewHolder {
        val view = parent.inflate(R.layout.item_shared_media_header, false)
        view.date_tv.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            val margin = parent.context.dpToPx(20f)
            marginStart = margin
            marginEnd = margin
        }
        return MediaHeaderViewHolder(view)
    }

    override fun onBindHeaderViewHolder(holder: MediaHeaderViewHolder, pos: Int) {
        val time = getItem(pos)?.createdAt ?: return
        holder.bind(time)
    }
}

class LinkHolder(itemView: View) : NormalHolder(itemView) {

    fun bind(item: HyperlinkItem, onClickListener: (url: String) -> Unit) {
        itemView.link_tv.text = item.hyperlink
        itemView.desc_tv.text = item.siteName
        itemView.setOnClickListener {
            item.hyperlink.let(onClickListener)
        }
    }
}
