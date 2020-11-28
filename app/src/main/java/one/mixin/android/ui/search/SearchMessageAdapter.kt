package one.mixin.android.ui.search

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemSearchMessageBinding
import one.mixin.android.extension.highLight
import one.mixin.android.extension.timeAgoDate
import one.mixin.android.ui.common.recyclerview.SafePagedListAdapter
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.SearchMessageDetailItem
import org.jetbrains.anko.dip

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
    val icon: Drawable? by lazy {
        AppCompatResources.getDrawable(itemView.context, R.drawable.ic_type_file).apply {
            this?.setBounds(0, 0, itemView.dip(12f), itemView.dip(12f))
        }
    }

    fun bind(
        message: SearchMessageDetailItem,
        query: String,
        searchMessageCallback: SearchMessageAdapter.SearchMessageCallback?
    ) {
        binding.searchNameTv.text = message.userFullName
        if (message.type == MessageCategory.SIGNAL_DATA.name || message.type == MessageCategory.PLAIN_DATA.name) {
            TextViewCompat.setCompoundDrawablesRelative(binding.searchMsgTv, icon, null, null, null)
            binding.searchMsgTv.text = message.mediaName
        } else if (message.type == MessageCategory.SIGNAL_CONTACT.name || message.type == MessageCategory.PLAIN_CONTACT.name) {
            TextViewCompat.setCompoundDrawablesRelative(binding.searchMsgTv, null, null, null, null)
            binding.searchMsgTv.text = message.mediaName
        } else {
            TextViewCompat.setCompoundDrawablesRelative(binding.searchMsgTv, null, null, null, null)
            binding.searchMsgTv.text = message.content
        }
        binding.searchTimeTv.timeAgoDate(message.createdAt)
        binding.searchMsgTv.highLight(query)
        binding.searchAvatarIv.setInfo(message.userFullName, message.userAvatarUrl, message.userId)
        itemView.setOnClickListener {
            searchMessageCallback?.onItemClick(message)
        }
    }
}
