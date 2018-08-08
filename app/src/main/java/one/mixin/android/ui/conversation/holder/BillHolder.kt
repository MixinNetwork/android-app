package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.date_wrapper.view.*
import kotlinx.android.synthetic.main.item_chat_bill.view.*
import one.mixin.android.R
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.vo.MessageItem

class BillHolder constructor(containerView: View) : BaseViewHolder(containerView) {

    init {
        itemView.chat_flag.visibility = View.GONE
    }

    private var onItemListener: ConversationAdapter.OnItemListener? = null

    fun bind(
        messageItem: MessageItem,
        isLast: Boolean,
        hasSelect: Boolean,
        isSelect: Boolean,
        onItemListener: ConversationAdapter.OnItemListener
    ) {
        this.onItemListener = onItemListener
        val isMe = meId == messageItem.userId
        chatLayout(isMe, isLast)
        itemView.chat_time.timeAgoClock(messageItem.createdAt)
        itemView.bill_iv.loadImage(messageItem.assetIcon, R.drawable.ic_avatar_place_holder)
        itemView.bill_tv.text = messageItem.snapshotAmount?.numberFormat8()
        itemView.bill_symbol_tv.text = messageItem.assetSymbol

        itemView.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(messageItem, adapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
                true
            }
        }
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
        itemView.chat_layout.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
            } else {
                onItemListener.onBillClick(messageItem)
            }
        }
        itemView.chat_layout.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(messageItem, adapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
                true
            }
        }
    }

    override fun chatLayout(isMe: Boolean, isLast: Boolean) {
        super.chatLayout(isMe, isLast)
        if (isMe) {
            if (isLast) {
                itemView.chat_layout.setBackgroundResource(R.drawable.bill_bubble_me_last)
            } else {
                itemView.chat_layout.setBackgroundResource(R.drawable.bill_bubble_me)
            }
            (itemView.chat_layout.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.END
        } else {
            (itemView.chat_layout.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.START
            if (isLast) {
                itemView.chat_layout.setBackgroundResource(R.drawable.chat_bubble_other_last)
            } else {
                itemView.chat_layout.setBackgroundResource(R.drawable.chat_bubble_other)
            }
        }
    }
}