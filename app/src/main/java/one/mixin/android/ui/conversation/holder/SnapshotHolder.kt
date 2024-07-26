package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.view.Gravity
import android.widget.FrameLayout
import one.mixin.android.Constants.Colors.SELECT_COLOR
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatSnapshotBinding
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.formatTo8DecimalsWithCommas
import one.mixin.android.extension.realSize
import one.mixin.android.ui.conversation.adapter.MessageAdapter
import one.mixin.android.ui.conversation.holder.base.BaseViewHolder
import one.mixin.android.vo.MessageItem

class SnapshotHolder constructor(val binding: ItemChatSnapshotBinding) : BaseViewHolder(binding.root) {
    init {
        binding.chatLayout.layoutParams.width = (itemView.context.realSize().x * 0.6).toInt()
    }

    private var onItemListener: MessageAdapter.OnItemListener? = null

    fun bind(
        messageItem: MessageItem,
        isLast: Boolean,
        hasSelect: Boolean,
        isSelect: Boolean,
        onItemListener: MessageAdapter.OnItemListener,
    ) {
        super.bind(messageItem)
        this.onItemListener = onItemListener
        val isMe = meId == messageItem.userId
        chatLayout(isMe, isLast)
        binding.billIv.loadImage(messageItem.assetIcon, R.drawable.ic_avatar_place_holder)
        val amount = messageItem.snapshotAmount
        if (!amount.isNullOrBlank()) {
            binding.billTv.text =
                if (amount.startsWith('-')) {
                    "-${amount.substring(1).formatTo8DecimalsWithCommas()}"
                } else {
                    amount.formatTo8DecimalsWithCommas()
                }
        }
        binding.billSymbolTv.text = messageItem.assetSymbol
        itemView.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(messageItem, absoluteAdapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
                true
            }
        }

        binding.chatTime.load(messageItem.createdAt)

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
        binding.chatLayout.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
            } else {
                onItemListener.onBillClick(messageItem)
            }
        }
        binding.chatLayout.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(messageItem, absoluteAdapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
                true
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
                    binding.chatLayout,
                    R.drawable.bill_bubble_me_last,
                    R.drawable.bill_bubble_me_last_night,
                )
            } else {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.bill_bubble_me,
                    R.drawable.bill_bubble_me_night,
                )
            }
            (binding.chatLayout.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.END
        } else {
            (binding.chatLayout.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.START
            if (isLast) {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_other_last,
                    R.drawable.chat_bubble_other_last_night,
                )
            } else {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_other,
                    R.drawable.chat_bubble_other_night,
                )
            }
        }
    }
}
