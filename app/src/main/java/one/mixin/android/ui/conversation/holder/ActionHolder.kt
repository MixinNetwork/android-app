package one.mixin.android.ui.conversation.holder

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import one.mixin.android.databinding.ItemChatActionBinding
import one.mixin.android.extension.bottomPadding
import one.mixin.android.extension.dp
import one.mixin.android.extension.leftPadding
import one.mixin.android.extension.rightPadding
import one.mixin.android.extension.topPadding
import one.mixin.android.moshi.MoshiHelper.getTypeListAdapter
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.ui.conversation.holder.base.BaseViewHolder
import one.mixin.android.util.ColorUtil
import one.mixin.android.vo.AppButtonData
import one.mixin.android.vo.MessageItem
import one.mixin.android.widget.ActionButton

class ActionHolder constructor(val binding: ItemChatActionBinding) : BaseViewHolder(binding.root) {

    @SuppressLint("RestrictedApi")
    fun bind(
        messageItem: MessageItem,
        isFirst: Boolean,
        hasSelect: Boolean,
        isSelect: Boolean,
        onItemListener: ConversationAdapter.OnItemListener
    ) {
        super.bind(messageItem)
        if (hasSelect && isSelect) {
            itemView.setBackgroundColor(SELECT_COLOR)
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT)
        }
        itemView.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
            }
        }

        itemView.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(messageItem, absoluteAdapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
                true
            }
        }
        val isMe = meId == messageItem.userId
        if (isFirst && !isMe) {
            binding.chatName.visibility = View.VISIBLE
            binding.chatName.text = messageItem.userFullName
            if (messageItem.appId != null) {
                binding.chatName.setCompoundDrawables(null, null, botIcon, null)
                binding.chatName.compoundDrawablePadding = 3.dp
            } else {
                binding.chatName.setCompoundDrawables(null, null, null, null)
            }
            binding.chatName.setTextColor(getColorById(messageItem.userId))
            binding.chatName.setOnClickListener { onItemListener.onUserClick(messageItem.userId) }
        } else {
            binding.chatName.visibility = View.GONE
        }
        if (itemView.tag != messageItem.content?.hashCode()) {
            val buttons = requireNotNull(getTypeListAdapter<List<AppButtonData>>(AppButtonData::class.java).fromJson(messageItem.content!!))
            binding.chatLayout.removeAllViews()
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
                binding.chatLayout.addView(button)
                (button.layoutParams as ViewGroup.MarginLayoutParams).marginStart = dp8
                button.topPadding = dp8
                button.bottomPadding = dp8
                button.leftPadding = dp12
                button.rightPadding = dp12
                button.setOnLongClickListener {
                    if (!hasSelect) {
                        onItemListener.onLongClick(messageItem, absoluteAdapterPosition)
                    } else {
                        true
                    }
                }
                button.setOnClickListener {
                    if (hasSelect) {
                        onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
                    } else {
                        onItemListener.onActionClick(b.action, messageItem.userId)
                    }
                }
            }
            itemView.tag = messageItem.content?.hashCode()
        }
    }
}
