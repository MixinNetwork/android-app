package one.mixin.android.ui.conversation.holder

import android.support.constraint.ConstraintLayout
import android.view.View
import kotlinx.android.synthetic.main.date_wrapper.view.*
import kotlinx.android.synthetic.main.item_chat_reply.view.*
import one.mixin.android.R
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.round
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.vo.MessageItem

class ReplyHolder constructor(containerView: View) : BaseViewHolder(containerView) {

    override fun chatLayout(isMe: Boolean, isLast: Boolean) {
        val dp8 = itemView.context.dpToPx(8f)
        val dp1 = itemView.context.dpToPx(1f)
        val lp = (itemView.chat_layout.layoutParams as ConstraintLayout.LayoutParams)
        if (isMe) {
            lp.horizontalBias = 1f
            if (isLast) {
                lp.bottomMargin = dp8
                itemView.chat_layout.setBackgroundResource(R.drawable.chat_bubble_image_me_last)
            } else {
                lp.bottomMargin = dp1
                itemView.chat_layout.setBackgroundResource(R.drawable.chat_bubble_image_me)
            }
        } else {
            lp.horizontalBias = 0f
            if (isLast) {
                lp.bottomMargin = dp8
                itemView.chat_layout.setBackgroundResource(R.drawable.chat_bubble_image_other_last)
            } else {
                lp.bottomMargin = dp1
                itemView.chat_layout.setBackgroundResource(R.drawable.chat_bubble_image_other)
            }
        }
    }

    init {
        val radius = itemView.context.dpToPx(4f).toFloat()
        itemView.reply_layout.round(radius)
    }

    fun bind(messageItem: MessageItem, isLast: Boolean) {
        itemView.chat_time.timeAgoClock(messageItem.createdAt)
        val isMe = meId == messageItem.userId
        chatLayout(isMe, isLast)
    }
}