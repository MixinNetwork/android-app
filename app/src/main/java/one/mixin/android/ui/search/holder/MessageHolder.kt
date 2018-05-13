package one.mixin.android.ui.search.holder

import android.graphics.drawable.Drawable
import android.support.v4.widget.TextViewCompat
import android.support.v7.content.res.AppCompatResources
import android.support.v7.widget.RecyclerView
import android.view.View
import kotlinx.android.synthetic.main.item_search_message.view.*
import one.mixin.android.R
import one.mixin.android.extension.timeAgo
import one.mixin.android.ui.search.SearchFragment
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageItem
import org.jetbrains.anko.dip

class MessageHolder constructor(containerView: View) : RecyclerView.ViewHolder(containerView) {
    val icon: Drawable? by lazy {
        AppCompatResources.getDrawable(itemView.context, R.drawable.ic_status_file).apply {
            this?.setBounds(0, 0, itemView.dip(12f), itemView.dip(12f))
        }
    }

    fun bind(message: MessageItem, onItemClickListener: SearchFragment.OnSearchClickListener?) {
        bind(message, onItemClickListener, false)
    }

    fun bind(message: MessageItem, onItemClickListener: SearchFragment.OnSearchClickListener?, isEnd: Boolean) {
        itemView.search_name_tv.text = message.userFullName
        if (message.type == MessageCategory.SIGNAL_DATA.name || message.type == MessageCategory.PLAIN_DATA.name) {
            TextViewCompat.setCompoundDrawablesRelative(itemView.search_msg_tv, icon, null, null, null)
            itemView.search_msg_tv.text = message.mediaName
        } else {
            TextViewCompat.setCompoundDrawablesRelative(itemView.search_msg_tv, null, null, null, null)
            itemView.search_msg_tv.text = message.content
        }
        itemView.search_time_tv.timeAgo(message.createdAt)
        itemView.search_avatar_iv.setInfo(if (message.userFullName.isNotEmpty()) message.userFullName[0]
        else ' ', message.userAvatarUrl, message.userId)
        itemView.divider.visibility = View.VISIBLE
        itemView.setOnClickListener {
            onItemClickListener?.onMessageClick(message)
        }
    }
}