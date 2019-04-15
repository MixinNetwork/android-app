package one.mixin.android.ui.search.holder

import android.graphics.drawable.Drawable
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_search_message.view.*
import one.mixin.android.R
import one.mixin.android.ui.search.SearchFragment
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.SearchMessageItem
import org.jetbrains.anko.dip

class MessageHolder constructor(containerView: View) : RecyclerView.ViewHolder(containerView) {
    val icon: Drawable? by lazy {
        AppCompatResources.getDrawable(itemView.context, R.drawable.ic_status_file).apply {
            this?.setBounds(0, 0, itemView.dip(12f), itemView.dip(12f))
        }
    }

    fun bind(message: SearchMessageItem, onItemClickListener: SearchFragment.OnSearchClickListener?) {
        itemView.search_name_tv.text = message.conversationName ?: message.userFullName
        itemView.search_msg_tv.text = itemView.context.getString(R.string.search_related_message, message.messageCount)
        if (message.conversationCategory == ConversationCategory.CONTACT.name) {
            if (message.botUserId != null && message.botUserId != message.userId && message.botFullName != null) {
                itemView.search_avatar_iv.setInfo(message.botFullName, message.botAvatarUrl, message.botUserId)
            } else if (message.userFullName != null) {
                itemView.search_avatar_iv.setInfo(message.userFullName, message.userAvatarUrl, message.userId)
            }
        } else {
            itemView.search_avatar_iv.setGroup(message.conversationAvatarUrl)
        }

        itemView.divider.visibility = View.VISIBLE
        itemView.setOnClickListener {
            onItemClickListener?.onMessageClick(message)
        }
    }
}