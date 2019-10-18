package one.mixin.android.ui.conversation.holder

import android.view.View
import kotlinx.android.synthetic.main.item_chat_unknown.view.*
import one.mixin.android.R
import one.mixin.android.extension.highLightClick
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.vo.MessageItem
import org.jetbrains.anko.dip

class UnknownHolder constructor(containerView: View) : BaseViewHolder(containerView) {

    override fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean) {
        super.chatLayout(isMe, isLast, isBlink)
        if (isLast) {
            itemView.chat_layout.setBackgroundResource(R.drawable.chat_bubble_other_last)
        } else {
            itemView.chat_layout.setBackgroundResource(R.drawable.chat_bubble_other)
        }
    }

    fun bind(
        messageItem: MessageItem,
        isLast: Boolean,
        isFirst: Boolean,
        onItemListener: ConversationAdapter.OnItemListener
    ) {
        itemView.chat_time.timeAgoClock(messageItem.createdAt)

        itemView.chat_tv.highLightClick(itemView.context.getString(R.string.unknown_update_app_link),
            action = {
                onItemListener.onUrlClick(itemView.context.getString(R.string.chat_unknown_url))
            })

        if (isFirst) {
            itemView.chat_name.visibility = View.VISIBLE
            itemView.chat_name.text = messageItem.userFullName
            if (messageItem.appId != null) {
                itemView.chat_name.setCompoundDrawables(null, null, botIcon, null)
                itemView.chat_name.compoundDrawablePadding = itemView.dip(3)
            } else {
                itemView.chat_name.setCompoundDrawables(null, null, null, null)
            }
            itemView.chat_name.setOnClickListener { onItemListener.onUserClick(messageItem.userId) }
            itemView.chat_name.setTextColor(getColorById(messageItem.userId))
        } else {
            itemView.chat_name.visibility = View.GONE
        }
        chatLayout(false, isLast)
    }
}
