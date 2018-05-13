package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import com.google.gson.Gson
import kotlinx.android.synthetic.main.item_chat_action.view.chat_name
import kotlinx.android.synthetic.main.item_chat_action.view.flow_layout
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.vo.AppButtonData
import one.mixin.android.vo.MessageItem
import one.mixin.android.widget.ActionButton
import org.jetbrains.anko.dip

class ActionHolder constructor(containerView: View) : BaseViewHolder(containerView) {

    fun bind(messageItem: MessageItem, isFirst: Boolean, onItemListener: ConversationAdapter.OnItemListener) {
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
            itemView.chat_name.setTextColor(colors[messageItem.userIdentityNumber.toLong().rem(colors.size).toInt()])
            itemView.chat_name.setOnClickListener { onItemListener.onUserClick(messageItem.userId) }
        } else {
            itemView.chat_name.visibility = View.GONE
        }
        if (itemView.tag != messageItem.content?.hashCode()) {
            val buttons = Gson().fromJson(messageItem.content, Array<AppButtonData>::class.java)
            itemView.flow_layout.removeAllViews()
            for (b in buttons) {
                val button = ActionButton(itemView.context)
                button.setTextColor(Color.parseColor(b.color))
                button.setTypeface(null, Typeface.BOLD)
                button.text = b.label
                itemView.flow_layout.addView(button)
                (button.layoutParams as ViewGroup.MarginLayoutParams).marginStart = button.dip(8)
                button.setOnClickListener { onItemListener.onActionClick(b.action) }
            }
            itemView.tag = messageItem.content?.hashCode()
        }
    }

    override fun chatLayout(isMe: Boolean, isLast: Boolean) {
    }
}