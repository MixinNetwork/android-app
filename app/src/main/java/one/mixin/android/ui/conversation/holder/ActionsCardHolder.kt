package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.graphics.Typeface
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import coil.load
import one.mixin.android.Constants.Colors.SELECT_COLOR
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatActionsCardBinding
import one.mixin.android.extension.doubleClickVibrate
import one.mixin.android.extension.dp
import one.mixin.android.extension.heavyClickVibrate
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.maxCardWidth
import one.mixin.android.ui.conversation.adapter.MessageAdapter
import one.mixin.android.ui.conversation.holder.base.BaseViewHolder
import one.mixin.android.util.ColorUtil
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.AppCardData
import one.mixin.android.vo.MessageItem
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

    private val radius by lazy {
        4.dp
    }

    fun bind(
        messageItem: MessageItem,
        isFirst: Boolean,
        isLast: Boolean,
        hasSelect: Boolean,
        isSelect: Boolean,
        isRepresentative: Boolean,
        onItemListener: MessageAdapter.OnItemListener,
    ) {
        super.bind(messageItem)
        val isMe = meId == messageItem.userId
        chatLayout(isMe, isLast)
        if (hasSelect && isSelect) {
            itemView.setBackgroundColor(SELECT_COLOR)
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT)
        }
        val longClickListener =
            View.OnLongClickListener {
                if (!hasSelect) {
                    onItemListener.onLongClick(messageItem, absoluteAdapterPosition)
                } else {
                    onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
                    true
                }
            }
        itemView.setOnLongClickListener(longClickListener)
        binding.chatLayout.setOnLongClickListener(longClickListener)
        chatJumpLayout(binding.chatJump, isMe, messageItem.expireIn, messageItem.expireAt, R.id.chat_layout)

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
        binding.chatContentLayout.setData(actionCard, isLast, isMe, messageItem.createdAt, messageItem.status, messageItem.isPin ?: false, isRepresentative, messageItem.isSecret(), onItemListener, textGestureListener)

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
            binding.chatLayout.setOnClickListener {
                if (hasSelect) {
                    onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
                }
            }
        } else {
            binding.chatGroupLayout.isVisible = false
            binding.chatLayout.setOnClickListener {
                if (hasSelect) {
                    onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
                }
            }
        }

        itemView.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
            }
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
        var messageItem: MessageItem,
        var hasSelect: Boolean = false,
        var isSelect: Boolean = false,
        var onItemListener: MessageAdapter.OnItemListener,
        var absoluteAdapterPosition: Int = 0,
    ) : GestureDetector.SimpleOnGestureListener() {
        var longPressed = false

        override fun onDoubleTap(e: MotionEvent): Boolean {
            view.context.doubleClickVibrate()
            onItemListener.onTextDoubleClick(messageItem)
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
            }
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            if (longPressed) {
                longPressed = false
                return
            }

            if (!hasSelect) {
                view.performLongClick()
            } else {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
            }
        }
    }
}