package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.view.View
import com.google.gson.Gson
import kotlinx.android.synthetic.main.item_chat_action_card.view.*
import one.mixin.android.R
import one.mixin.android.extension.loadRoundImage
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.vo.AppCardData
import one.mixin.android.vo.MessageItem
import org.jetbrains.anko.dip

class ActionCardHolder constructor(containerView: View) : BaseViewHolder(containerView) {
    private val radius by lazy {
        itemView.context.dip(4)
    }

    fun bind(
        messageItem: MessageItem,
        isFirst: Boolean,
        hasSelect: Boolean,
        isSelect: Boolean,
        onItemListener: ConversationAdapter.OnItemListener
    ) {
        val isMe = meId == messageItem.userId
        if (hasSelect && isSelect) {
            itemView.setBackgroundColor(SELECT_COLOR)
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT)
        }
        itemView.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(messageItem, adapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
                true
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
            itemView.chat_name.setTextColor(colors[messageItem.userIdentityNumber.toLong().rem(colors.size).toInt()])
            itemView.chat_name.setOnClickListener { onItemListener.onUserClick(messageItem.userId) }
        } else {
            itemView.chat_name.visibility = View.GONE
        }
        val actionCard = Gson().fromJson(messageItem.content, AppCardData::class.java)
        itemView.chat_icon.loadRoundImage(actionCard.iconUrl, radius, R.drawable.holder_bot)
        itemView.chat_title.text = actionCard.title
        itemView.chat_description.text = actionCard.description
        itemView.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
            } else {
                onItemListener.onActionClick(actionCard.action)
            }
        }
    }

    override fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean) {
        super.chatLayout(isMe, isLast, isBlink)
    }
}