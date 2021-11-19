package one.mixin.android.ui.conversation.chathistory.holder

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatActionBinding
import one.mixin.android.session.Session
import one.mixin.android.ui.conversation.chathistory.TranscriptAdapter
import one.mixin.android.util.ColorUtil
import one.mixin.android.util.MoshiHelper
import one.mixin.android.vo.AppButtonData
import one.mixin.android.vo.ChatHistoryMessageItem
import one.mixin.android.widget.ActionButton
import org.jetbrains.anko.bottomPadding
import org.jetbrains.anko.dip
import org.jetbrains.anko.leftPadding
import org.jetbrains.anko.rightPadding
import org.jetbrains.anko.topPadding

class ActionHolder constructor(val binding: ItemChatActionBinding) : BaseViewHolder(binding.root) {

    @SuppressLint("RestrictedApi")
    fun bind(
        messageItem: ChatHistoryMessageItem,
        isLast: Boolean,
        isFirst: Boolean = false,
        onItemListener: TranscriptAdapter.OnItemListener
    ) {
        super.bind(messageItem)
        val isMe = messageItem.userId == Session.getAccountId()
        chatLayout(isMe, isLast)
        if (isFirst && !isMe) {
            binding.chatName.visibility = View.VISIBLE
            binding.chatName.text = messageItem.userFullName
            if (messageItem.appId != null) {
                binding.chatName.setCompoundDrawables(null, null, botIcon, null)
                binding.chatName.compoundDrawablePadding = itemView.dip(3)
            } else {
                binding.chatName.setCompoundDrawables(null, null, null, null)
            }
            binding.chatName.setTextColor(getColorById(messageItem.userId))
            binding.chatName.setOnClickListener { onItemListener.onUserClick(messageItem.userId) }
        } else {
            binding.chatName.visibility = View.GONE
        }
        if (itemView.tag != messageItem.content?.hashCode()) {
            val buttons = requireNotNull(
                MoshiHelper.getTypeListAdapter<List<AppButtonData>>(
                    AppButtonData::class.java
                ).fromJson(messageItem.content!!)
            )
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
                button.setOnClickListener {
                    onItemListener.onActionClick(b.action, messageItem.userId)
                }
            }
            itemView.tag = messageItem.content?.hashCode()
        }
        if (messageItem.transcriptId == null) {
            binding.root.setOnLongClickListener {
                onItemListener.onMenu(binding.chatJump, messageItem)
                true
            }
            binding.chatLayout.setOnLongClickListener {
                onItemListener.onMenu(binding.chatJump, messageItem)
                true
            }
            chatJumpLayout(binding.chatJump, isMe, messageItem.messageId, R.id.chat_layout, onItemListener)
        }
    }
}
