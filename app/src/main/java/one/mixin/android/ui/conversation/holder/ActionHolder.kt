package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_chat_action.view.*
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.util.ColorUtil
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.AppButtonData
import one.mixin.android.vo.MessageItem
import one.mixin.android.widget.ActionButton
import org.jetbrains.anko.dip

class ActionHolder constructor(containerView: View) : BaseViewHolder(containerView) {

    fun bind(
        messageItem: MessageItem,
        isFirst: Boolean,
        hasSelect: Boolean,
        isSelect: Boolean,
        onItemListener: ConversationAdapter.OnItemListener
    ) {
        if (hasSelect && isSelect) {
            itemView.setBackgroundColor(SELECT_COLOR)
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT)
        }
        itemView.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
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
        if (itemView.tag != messageItem.content?.hashCode()) {
            val buttons = GsonHelper.customGson.fromJson(messageItem.content, Array<AppButtonData>::class.java)
            itemView.flow_layout.removeAllViews()
            for (b in buttons) {
                val button = ActionButton(itemView.context)
                button.setTextColor(
                    try {
                        ColorUtil.parseColor(b.color.trim())
                    } catch (e: Throwable) {
                        Color.BLACK
                    }
                )
                button.setTypeface(null, Typeface.BOLD)
                button.text = b.label
                itemView.flow_layout.addView(button)
                (button.layoutParams as ViewGroup.MarginLayoutParams).marginStart =
                    button.dip(8)
                button.setOnLongClickListener {
                    if (!hasSelect) {
                        onItemListener.onLongClick(messageItem, adapterPosition)
                    } else {
                        true
                    }
                }
                button.setOnClickListener {
                    if (hasSelect) {
                        onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
                    } else {
                        onItemListener.onActionClick(b.action, messageItem.userId)
                    }
                }
            }
            itemView.tag = messageItem.content?.hashCode()
        }
    }
}
