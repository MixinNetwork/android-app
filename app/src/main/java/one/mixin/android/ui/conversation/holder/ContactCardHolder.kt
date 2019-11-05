package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.date_wrapper.view.*
import kotlinx.android.synthetic.main.item_chat_contact_card.view.*
import one.mixin.android.R
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.util.Session
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.showVerifiedOrBot
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
        if (hasSelect && isSelect) {
            itemView.setBackgroundColor(SELECT_COLOR)
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT)
        }
        itemView.avatar_iv.setInfo(item.sharedUserFullName, item.sharedUserAvatarUrl, item.sharedUserId
            ?: "0")
        itemView.name_tv.text = item.sharedUserFullName
        itemView.id_tv.text = item.sharedUserIdentityNumber
        itemView.chat_time.timeAgoClock(item.createdAt)
        item.showVerifiedOrBot(itemView.verified_iv, itemView.bot_iv)

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
            itemView.chat_name.setTextColor(getColorById(item.userId))
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

        itemView.chat_layout.setOnClickListener {
            if (!hasSelect) {
                onItemListener.onContactCardClick(item.sharedUserId!!)
            } else {
                onItemListener.onSelect(!isSelect, item, adapterPosition)
            }
        }
        itemView.setOnClickListener {
            if (hasSelect) {
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
        itemView.chat_layout.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(item, adapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, item, adapterPosition)
                true
            }
        }
    }

    override fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean) {
        super.chatLayout(isMe, isLast, isBlink)
        if (isMe) {
            if (isLast) {
                setItemBackgroundResource(
                    itemView.chat_layout,
                    R.drawable.bill_bubble_me_last,
                    R.drawable.bill_bubble_me_last_night
                )
            } else {
                 setItemBackgroundResource(
                    itemView.chat_layout,
                    R.drawable.bill_bubble_me,
                    R.drawable.bill_bubble_me_night
                )
            }
            (itemView.out_ll.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.END
        } else {
            (itemView.out_ll.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.START
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
    }
}
