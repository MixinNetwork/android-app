package one.mixin.android.ui.conversation.holder

import android.view.View
import kotlinx.android.synthetic.main.item_chat_waiting.view.*
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.highlightLinkText
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.vo.MessageItem
import org.jetbrains.anko.dip

class WaitingHolder constructor(
    containerView: View,
    private val onItemListener: ConversationAdapter.OnItemListener
) : BaseViewHolder(containerView) {

    override fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean) {
        super.chatLayout(isMe, isLast, isBlink)
        if (isLast) {
        setItemBackgroundResource(
                    itemView.chat_layout,
                    R.drawable.chat_bubble_other_last,
                    R.drawable.chat_bubble_other_last_night
                )
        } else {
         setItemBackgroundResource(
                    itemView.chat_layout,
                    R.drawable.chat_bubble_other,
                    R.drawable.chat_bubble_other_night
                )
        }
    }

    fun bind(
        messageItem: MessageItem,
        isLast: Boolean,
        isFirst: Boolean,
        onItemListener: ConversationAdapter.OnItemListener
    ) {
        itemView.chat_time.timeAgoClock(messageItem.createdAt)

        val learn: String = MixinApplication.get().getString(R.string.chat_learn)
        val info = MixinApplication.get().getString(R.string.chat_waiting, messageItem.userFullName, learn)
        val learnUrl = MixinApplication.get().getString(R.string.chat_waiting_url)
        itemView.chat_tv.highlightLinkText(
            info,
            arrayOf(learn),
            arrayOf(learnUrl))

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
