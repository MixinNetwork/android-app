package one.mixin.android.ui.media

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.paging.PagedListAdapter
import com.jakewharton.rxbinding3.view.clicks
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import io.reactivex.android.schedulers.AndroidSchedulers
import one.mixin.android.R
import one.mixin.android.databinding.ItemSharedMediaHeaderBinding
import one.mixin.android.databinding.ItemSharedMediaLinkBinding
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.hashForDate
import one.mixin.android.extension.inflate
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.vo.HyperlinkItem
import java.util.concurrent.TimeUnit
import kotlin.math.abs

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
        val binding = ItemSharedMediaHeaderBinding.bind(view)
        binding.dateTv.updateLayoutParams<ViewGroup.MarginLayoutParams> {
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
    private val binding = ItemSharedMediaLinkBinding.bind(itemView)

    @SuppressLint("CheckResult")
    fun bind(item: HyperlinkItem, onClickListener: (url: String) -> Unit) {
        binding.linkTv.text = item.hyperlink
        itemView.clicks()
            .observeOn(AndroidSchedulers.mainThread())
            .throttleFirst(1, TimeUnit.SECONDS)
            .subscribe {
                item.hyperlink.let(onClickListener)
            }
    }
}
