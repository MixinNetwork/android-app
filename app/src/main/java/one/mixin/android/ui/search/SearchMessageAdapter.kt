package one.mixin.android.ui.search

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_search_message.view.*
import one.mixin.android.R
import one.mixin.android.extension.highLight
import one.mixin.android.extension.timeAgoDate
import one.mixin.android.ui.common.recyclerview.SafePagedListAdapter
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.SearchMessageDetailItem
import org.jetbrains.anko.dip

class SearchMessageAdapter : SafePagedListAdapter<SearchMessageDetailItem, SearchMessageHolder>(SearchMessageDetailItem.DIFF_CALLBACK) {
    var query: String = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        SearchMessageHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_search_message, parent, false))

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

class SearchMessageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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
        itemView.search_name_tv.text = message.userFullName
        if (message.type == MessageCategory.SIGNAL_DATA.name || message.type == MessageCategory.PLAIN_DATA.name) {
            TextViewCompat.setCompoundDrawablesRelative(itemView.search_msg_tv, icon, null, null, null)
            itemView.search_msg_tv.text = message.mediaName
        } else if (message.type == MessageCategory.SIGNAL_CONTACT.name || message.type == MessageCategory.PLAIN_CONTACT.name) {
            TextViewCompat.setCompoundDrawablesRelative(itemView.search_msg_tv, null, null, null, null)
            itemView.search_msg_tv.text = message.mediaName
        } else {
            TextViewCompat.setCompoundDrawablesRelative(itemView.search_msg_tv, null, null, null, null)
            itemView.search_msg_tv.text = message.content
        }
        itemView.search_time_tv.timeAgoDate(message.createdAt)
        itemView.search_msg_tv.highLight(query)
        itemView.search_avatar_iv.setInfo(message.userFullName, message.userAvatarUrl, message.userId)
        itemView.setOnClickListener {
            searchMessageCallback?.onItemClick(message)
        }
    }
}
