package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.date_wrapper.view.*
import kotlinx.android.synthetic.main.item_chat_location.view.*
import one.mixin.android.R
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.maxItemWidth
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.Location
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.isSignal
import org.jetbrains.anko.dip

class LocationHolder constructor(containerView: View) : BaseViewHolder(containerView) {
    private val dp16 = itemView.context.dpToPx(16f)

    init {
        itemView.chat_name.maxWidth = itemView.context.maxItemWidth() - dp16
    }

    override fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean) {
        super.chatLayout(isMe, isLast, isBlink)
        val lp = (itemView.chat_msg_layout.layoutParams as FrameLayout.LayoutParams)
        if (isMe) {
            lp.gravity = Gravity.END
            if (isLast) {
                setItemBackgroundResource(
                    itemView.chat_layout,
                    R.drawable.chat_bubble_reply_me_last,
                    R.drawable.chat_bubble_reply_me_last_night
                )
            } else {
                setItemBackgroundResource(
                    itemView.chat_layout,
                    R.drawable.chat_bubble_reply_me,
                    R.drawable.chat_bubble_reply_me_night
                )
            }
        } else {
            lp.gravity = Gravity.START
            if (isLast) {
                setItemBackgroundResource(
                    itemView.chat_layout,
                    R.drawable.chat_bubble_reply_other_last,
                    R.drawable.chat_bubble_reply_other_last_night
                )
            } else {
                setItemBackgroundResource(
                    itemView.chat_layout,
                    R.drawable.chat_bubble_reply_other,
                    R.drawable.chat_bubble_reply_other_night
                )
            }
        }
    }

    private var onItemListener: ConversationAdapter.OnItemListener? = null

    fun bind(
        messageItem: MessageItem,
        isLast: Boolean,
        isFirst: Boolean = false,
        hasSelect: Boolean,
        isSelect: Boolean,
        onItemListener: ConversationAdapter.OnItemListener
    ) {
        this.onItemListener = onItemListener

        val location = GsonHelper.customGson.fromJson(messageItem.content!!, Location::class.java)
        itemView.location_title.text = "${location.latitude}"
        itemView.location_sub_title.text = "${location.latitude}"
        if (hasSelect && isSelect) {
            itemView.setBackgroundColor(SELECT_COLOR)
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT)
        }

        itemView.chat_layout.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(messageItem, adapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
                true
            }
        }

        itemView.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(messageItem, adapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
                true
            }
        }

        itemView.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
            }
        }

        itemView.chat_time.timeAgoClock(messageItem.createdAt)

        val isMe = meId == messageItem.userId
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

        if (messageItem.appId != null) {
            itemView.chat_name.setCompoundDrawables(null, null, botIcon, null)
            itemView.chat_name.compoundDrawablePadding = itemView.dip(3)
        } else {
            itemView.chat_name.setCompoundDrawables(null, null, null, null)
        }
        setStatusIcon(isMe, messageItem.status, messageItem.isSignal()) { statusIcon, secretIcon ->
            itemView.chat_flag.isVisible = statusIcon != null
            itemView.chat_flag.setImageDrawable(statusIcon)
            itemView.chat_secret.isVisible = secretIcon != null
        }
        itemView.chat_secret.isVisible = messageItem.isSignal()
        itemView.chat_layout.setOnClickListener {
            if (!hasSelect) {
                onItemListener.onMessageClick(messageItem.quoteId)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
            }
        }

        chatLayout(isMe, isLast)
    }
}
