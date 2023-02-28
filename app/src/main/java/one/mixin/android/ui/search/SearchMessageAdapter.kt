package one.mixin.android.ui.search

import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemSearchMessageBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.highLight
import one.mixin.android.extension.timeAgoDate
import one.mixin.android.ui.common.recyclerview.SafePagedListAdapter
import one.mixin.android.vo.SearchMessageDetailItem
import one.mixin.android.vo.isContact
import one.mixin.android.vo.isData
import one.mixin.android.vo.isTranscript
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class SearchMessageAdapter : SafePagedListAdapter<SearchMessageDetailItem, SearchMessageHolder>(SearchMessageDetailItem.DIFF_CALLBACK) {
    var query: String = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        SearchMessageHolder(ItemSearchMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: SearchMessageHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it, query, callback)
        }
    }

    var callback: SearchMessageCallback? = null

    interface SearchMessageCallback {
        fun onItemClick(item: SearchMessageDetailItem)
    }
}

class SearchMessageHolder(val binding: ItemSearchMessageBinding) : RecyclerView.ViewHolder(binding.root) {
    private val fileIcon: Drawable? by lazy {
        AppCompatResources.getDrawable(itemView.context, R.drawable.ic_type_file).apply {
            this?.setBounds(0, 0, 12f.dp, 12f.dp)
        }
    }

    private val contactIcon: Drawable? by lazy {
        AppCompatResources.getDrawable(itemView.context, R.drawable.ic_type_contact).apply {
            this?.setBounds(0, 0, 12f.dp, 12f.dp)
        }
    }

    fun bind(
        message: SearchMessageDetailItem,
        query: String,
        searchMessageCallback: SearchMessageAdapter.SearchMessageCallback?,
    ) {
        binding.searchNameTv.text = message.userFullName
        if (message.isData()) {
            TextViewCompat.setCompoundDrawablesRelative(binding.searchMsgTv, fileIcon, null, null, null)
            binding.searchMsgTv.text = message.mediaName
        } else if (message.isContact()) {
            TextViewCompat.setCompoundDrawablesRelative(binding.searchMsgTv, contactIcon, null, null, null)
            binding.searchMsgTv.text = message.mediaName
        } else if (message.isTranscript()) {
            TextViewCompat.setCompoundDrawablesRelative(binding.searchMsgTv, fileIcon, null, null, null)
            binding.searchMsgTv.text = binding.searchMsgTv.context.getString(R.string.Transcript)
        } else {
            TextViewCompat.setCompoundDrawablesRelative(binding.searchMsgTv, null, null, null, null)
            binding.searchMsgTv.text = message.content
        }
        customEllipsize(binding.searchMsgTv, query)
        binding.searchTimeTv.timeAgoDate(message.createdAt)
        binding.searchAvatarIv.setInfo(message.userFullName, message.userAvatarUrl, message.userId)
        itemView.setOnClickListener {
            searchMessageCallback?.onItemClick(message)
        }
    }

    private fun customEllipsize(tv: TextView, query: String) {
        var globalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null
        globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            val layout = tv.layout
            if (layout != null) {
                globalLayoutListener?.let {
                    tv.viewTreeObserver.removeOnGlobalLayoutListener(it)
                }
                val visibleWidth = abs(layout.getLineEnd(0) - layout.getLineStart(0))
                val textLength = tv.text.length
                val queryLength = query.length
                if (visibleWidth >= textLength) {
                    tv.highLight(query)
                } else if (queryLength > visibleWidth) {
                    tv.ellipsize = TextUtils.TruncateAt.MIDDLE
                    tv.highLight(query)
                } else {
                    val queryIndex = tv.text.indexOf(query)
                    val offset = (visibleWidth - queryLength) / 2
                    if (queryIndex + queryLength + offset >= textLength) {
                        tv.ellipsize = TextUtils.TruncateAt.START
                        tv.highLight(query, source = tv.text.substring(max(0, textLength - queryIndex - 3), textLength))
                    } else if (queryIndex - offset <= 0) {
                        tv.ellipsize = TextUtils.TruncateAt.END
                        tv.highLight(query, source = tv.text.substring(0, min(visibleWidth + 3, textLength)))
                    } else {
                        tv.ellipsize = TextUtils.TruncateAt.END
                        tv.highLight(query, source = tv.text.substring(max(0, queryIndex - offset - 3), min(textLength, queryIndex + queryLength + offset + 3)))
                    }
                }
            }
        }
        tv.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
    }
}
