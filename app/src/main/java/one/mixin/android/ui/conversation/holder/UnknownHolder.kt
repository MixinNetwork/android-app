package one.mixin.android.ui.conversation.holder

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.item_chat_unknown.view.*
import one.mixin.android.R
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.util.Session
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.isSignal
import org.jetbrains.anko.dip

class UnknownHolder constructor(containerView: View) : BaseViewHolder(containerView) {

    fun bind(messageItem: MessageItem, isFirst: Boolean, isLast: Boolean, onItemListener: ConversationAdapter.OnItemListener) {
        val isMe = messageItem.userId == Session.getAccountId()
        val lp = (itemView.chat_layout.layoutParams as ConstraintLayout.LayoutParams)
        if (isMe) {
            lp.horizontalBias = 1f
            if (isLast) {
                itemView.chat_layout.setBackgroundResource(R.drawable.chat_bubble_unknown_me_last)
            } else {
                itemView.chat_layout.setBackgroundResource(R.drawable.chat_bubble_unknown_me)
            }
        } else {
            lp.horizontalBias = 0f
            if (isLast) {
                itemView.chat_layout.setBackgroundResource(R.drawable.chat_bubble_unknown_other_last)
            } else {
                itemView.chat_layout.setBackgroundResource(R.drawable.chat_bubble_unknown_other)
            }
        }
        if (isFirst && !isMe) {
            itemView.chat_name.visibility = View.VISIBLE
            itemView.chat_name.text = messageItem.userFullName
            if (messageItem.appId != null) {
                itemView.chat_name.setCompoundDrawables(null, null, botIcon, null)
                itemView.chat_name.compoundDrawablePadding = itemView.dip(3)
            } else {
                itemView.chat_name.setCompoundDrawables(null, null, null, null)
            }
            itemView.chat_name.setTextColor(getColorById(messageItem.userId))
            itemView.chat_name.setOnClickListener { onItemListener.onUserClick(messageItem.userId) }
        } else {
            itemView.chat_name.visibility = View.GONE
        }
        itemView.chat_time.timeAgoClock(messageItem.createdAt)
        itemView.chat_secret.isVisible = messageItem.isSignal()
    }
}
