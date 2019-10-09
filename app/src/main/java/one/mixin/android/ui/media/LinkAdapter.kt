package one.mixin.android.ui.media

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import kotlin.math.abs
import kotlinx.android.synthetic.main.item_shared_media_link.view.*
import one.mixin.android.R
import one.mixin.android.extension.hashForDate
import one.mixin.android.extension.inflate
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.ui.common.recyclerview.PagedHeaderAdapter
import one.mixin.android.vo.HyperlinkItem

class LinkAdapter(private val onClickListener: (url: String) -> Unit) :
    PagedHeaderAdapter<HyperlinkItem, LinkHolder>(HyperlinkItem.DIFF_CALLBACK),
    StickyRecyclerHeadersAdapter<MediaHeaderViewHolder> {

    override fun getNormalViewHolder(context: Context, parent: ViewGroup) =
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
        return if (headerView != null && pos == TYPE_HEADER) {
            -1
        } else {
            val hyperlink = getItem(getPos(pos))
            abs(hyperlink?.createdAt?.hashForDate() ?: -1)
        }
    }

    override fun onCreateHeaderViewHolder(parent: ViewGroup) =
        MediaHeaderViewHolder(parent.inflate(R.layout.item_shared_media_header, false))

    override fun onBindHeaderViewHolder(holder: MediaHeaderViewHolder, pos: Int) {
        val time = getItem(getPos(pos))?.createdAt ?: return
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
