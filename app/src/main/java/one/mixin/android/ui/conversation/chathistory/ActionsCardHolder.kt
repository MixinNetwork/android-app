package one.mixin.android.ui.conversation.chathistory

import android.graphics.Color
import android.graphics.Typeface
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatActionsCardBinding
import one.mixin.android.extension.doubleClickVibrate
import one.mixin.android.extension.dp
import one.mixin.android.extension.heavyClickVibrate
import one.mixin.android.extension.maxCardWidth
import one.mixin.android.ui.conversation.chathistory.holder.BaseViewHolder
import one.mixin.android.ui.conversation.holder.AppCard
import one.mixin.android.util.ColorUtil
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.AppCardData
import one.mixin.android.vo.ChatHistoryMessageItem
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.isSecret
import one.mixin.android.widget.ActionButton

class ActionsCardHolder(val binding: ItemChatActionsCardBinding) :
    BaseViewHolder(binding.root) {
    init {
        binding.chatGroupLayout.layoutParams = binding.chatGroupLayout.layoutParams.apply {
            width = itemView.context.maxCardWidth() - 6.dp
        }
        binding.chatGroupLayout.setLineSpacing(6.dp)
    }

    fun bind(
        messageItem: ChatHistoryMessageItem,
        isLast: Boolean,
        isFirst: Boolean = false,
        onItemListener: ChatHistoryAdapter.OnItemListener,
    ) {
        super.bind(messageItem)
        val isMe = meId == messageItem.userId
        chatLayout(isMe, isLast)

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
        val actionCard = messageItem.appCardData?:return
        binding.chatContentLayout.setData(actionCard, isLast, isMe, messageItem.createdAt, MessageStatus.DELIVERED.name, isPin = false, isRepresentative = false, isSecret = false, onItemListener = onItemListener, textGestureListener = textGestureListener)

        binding.chatGroupLayout.removeAllViews()
        if (!actionCard.actions.isNullOrEmpty()) {
            binding.chatGroupLayout.isVisible = true
            for (b in actionCard.actions) {
                val button = ActionButton(itemView.context, b.externalLink, b.sendLink)
                button.setTextColor(
                    try {
                        ColorUtil.parseColor(b.color.trim())
                    } catch (e: Throwable) {
                        Color.BLACK
                    },
                )
                button.setTypeface(null, Typeface.BOLD)
                button.setText(b.label)
                binding.chatGroupLayout.addView(button)
                button.setOnClickListener {
                    onItemListener.onActionClick(b.action, messageItem.userId)
                }
            }
        } else {
            binding.chatGroupLayout.isVisible = false
            binding.chatLayout.setOnClickListener {
                onItemListener.onAppCardClick(actionCard, messageItem.userId)
            }
        }
        if (messageItem.transcriptId == null) {
            binding.root.setOnLongClickListener {
                onItemListener.onMenu(binding.chatJump, messageItem)
                true
            }
            binding.chatContentLayout.setOnLongClickListener {
                onItemListener.onMenu(binding.chatJump, messageItem)
                true
            }

            chatJumpLayout(binding.chatJump, isMe, messageItem.messageId, R.id.chat_layout, onItemListener)
        }
    }

    override fun chatLayout(
        isMe: Boolean,
        isLast: Boolean,
        isBlink: Boolean,
    ) {
        super.chatLayout(isMe, isLast, isBlink)
        if (isMe) {
            if (isLast) {
                setItemBackgroundResource(
                    binding.chatContentLayout,
                    R.drawable.chat_bubble_post_me_last,
                    R.drawable.chat_bubble_post_me_last_night,
                )
            } else {
                setItemBackgroundResource(
                    binding.chatContentLayout,
                    R.drawable.chat_bubble_post_me,
                    R.drawable.chat_bubble_post_me_night,
                )
            }
            (binding.chatLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 1f
            (binding.chatGroupLayout.layoutParams as MarginLayoutParams).marginStart = 6.dp
            (binding.chatGroupLayout.layoutParams as MarginLayoutParams).marginEnd = 8.dp
        } else {
            (binding.chatLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 0f
            (binding.chatGroupLayout.layoutParams as MarginLayoutParams).marginStart = 12.dp
            (binding.chatGroupLayout.layoutParams as MarginLayoutParams).marginEnd = 8.dp
            if (isLast) {
                setItemBackgroundResource(
                    binding.chatContentLayout,
                    R.drawable.chat_bubble_post_other_last,
                    R.drawable.chat_bubble_post_other_last_night,
                )
            } else {
                setItemBackgroundResource(
                    binding.chatContentLayout,
                    R.drawable.chat_bubble_post_other,
                    R.drawable.chat_bubble_post_other_night,
                )
            }
        }
    }

    private var textGestureListener: TextGestureListener? = null

    class TextGestureListener(
        var view: View,
        var messageItem: ChatHistoryMessageItem,
        var onItemListener: ChatHistoryAdapter.OnItemListener,
        var absoluteAdapterPosition: Int = 0,
    ) : GestureDetector.SimpleOnGestureListener() {
        var longPressed = false

        override fun onDoubleTap(e: MotionEvent): Boolean {
            view.context.doubleClickVibrate()
            onItemListener.onTextDoubleClick(messageItem)
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            if (longPressed) {
                longPressed = false
                return
            }
        }
    }
}