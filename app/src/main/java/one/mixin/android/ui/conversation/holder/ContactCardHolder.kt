package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.date_wrapper.view.*
import kotlinx.android.synthetic.main.item_chat_contact_card.view.*
import one.mixin.android.R
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.util.Session
import one.mixin.android.vo.MessageItem
import org.jetbrains.anko.dip

class ContactCardHolder(containerView: View) : BaseViewHolder(containerView) {

    fun bind(
        item: MessageItem,
        isFirst: Boolean,
        isLast: Boolean,
        hasSelect: Boolean,
        isSelect: Boolean,
        onItemListener: ConversationAdapter.OnItemListener
    ) {
        listen(item.messageId)
        if (hasSelect && isSelect) {
            itemView.setBackgroundColor(SELECT_COLOR)
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT)
        }
        itemView.avatar_iv.setInfo(item.sharedUserFullName, item.sharedUserAvatarUrl, item.sharedUserIdentityNumber
            ?: "0")
        itemView.name_tv.text = item.sharedUserFullName
        itemView.id_tv.text = item.sharedUserIdentityNumber
        itemView.chat_time.timeAgoClock(item.createdAt)
        itemView.verified_iv.visibility = if (item.sharedUserIsVerified == true) VISIBLE else GONE
        itemView.bot_iv.visibility = if (item.sharedUserAppId != null) VISIBLE else GONE

        val isMe = Session.getAccountId() == item.userId
        if (isFirst && !isMe) {
            itemView.chat_name.visibility = View.VISIBLE
            itemView.chat_name.text = item.userFullName
            if (item.appId != null) {
                itemView.chat_name.setCompoundDrawables(null, null, botIcon, null)
                itemView.chat_name.compoundDrawablePadding = itemView.dip(3)
            } else {
                itemView.chat_name.setCompoundDrawables(null, null, null, null)
            }
            itemView.chat_name.setTextColor(colors[item.userIdentityNumber.toLong().rem(colors.size).toInt()])
            itemView.chat_name.setOnClickListener { onItemListener.onUserClick(item.userId) }
        } else {
            itemView.chat_name.visibility = View.GONE
        }

        setStatusIcon(isMe, item.status, {
            itemView.chat_flag.setImageDrawable(it)
            itemView.chat_flag.visibility = View.VISIBLE
        }, {
            itemView.chat_flag.visibility = View.GONE
        })
        chatLayout(isMe, isLast)

        itemView.setOnClickListener {
            if (!hasSelect) {
                onItemListener.onContactCardClick(item.sharedUserId!!)
            } else {
                onItemListener.onSelect(!isSelect, item, adapterPosition)
            }
        }
        itemView.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(item, adapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, item, adapterPosition)
                true
            }
        }
    }

    override fun chatLayout(isMe: Boolean, isLast: Boolean) {
        if (isMe) {
            if (isLast) {
                itemView.chat_layout.setBackgroundResource(R.drawable.bill_bubble_me_last)
            } else {
                itemView.chat_layout.setBackgroundResource(R.drawable.bill_bubble_me)
            }
            (itemView.out_ll.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.END
        } else {
            (itemView.out_ll.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.START
            if (isLast) {
                itemView.chat_layout.setBackgroundResource(R.drawable.chat_bubble_other_last)
            } else {
                itemView.chat_layout.setBackgroundResource(R.drawable.chat_bubble_other)
            }
        }
    }
}