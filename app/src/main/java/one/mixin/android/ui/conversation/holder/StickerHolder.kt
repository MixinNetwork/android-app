package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.support.v4.widget.TextViewCompat
import android.support.v7.content.res.AppCompatResources
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.item_chat_sticker.view.*
import one.mixin.android.R
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.MessageStatus
import org.jetbrains.anko.dip
import org.jetbrains.anko.textColorResource

class StickerHolder constructor(containerView: View) : BaseViewHolder(containerView) {

    init {
        itemView.chat_time.textColorResource = R.color.color_chat_date
    }

    private val dp10 by lazy {
        itemView.context.dpToPx(10f)
    }

    private val dp160 by lazy {
        itemView.context.dpToPx(160f)
    }

    private val dp100 by lazy {
        itemView.context.dpToPx(100f)
    }

    fun bind(
        messageItem: MessageItem,
        isFirst: Boolean,
        hasSelect: Boolean,
        isSelect: Boolean,
        onItemListener: ConversationAdapter.OnItemListener
    ) {
        val isMe = meId == messageItem.userId
        chatLayout(isMe, false)
        if (hasSelect && isSelect) {
            itemView.setBackgroundColor(Color.parseColor("#660D94FC"))
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
        itemView.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
            }
        }
        if (messageItem.assetWidth == null || messageItem.assetHeight == null) {
            itemView.chat_sticker.layoutParams.width = dp100
            itemView.chat_time.layoutParams.width = dp100
            itemView.chat_sticker.layoutParams.height = dp100
            itemView.chat_sticker.setImageDrawable(ColorDrawable(Color.TRANSPARENT))
        } else if (messageItem.assetWidth > dp160) {
            itemView.chat_sticker.layoutParams.width = dp160
            itemView.chat_time.layoutParams.width = dp160
            itemView.chat_sticker.layoutParams.height = dp160 * messageItem.assetHeight / messageItem.assetWidth
            itemView.chat_sticker.loadImage(messageItem.assetUrl)
        } else {
            itemView.chat_sticker.layoutParams.width = messageItem.assetWidth
            itemView.chat_time.layoutParams.width = messageItem.assetWidth
            itemView.chat_sticker.layoutParams.height = messageItem.assetHeight
            itemView.chat_sticker.loadImage(messageItem.assetUrl)
        }

        itemView.chat_time.timeAgoClock(messageItem.createdAt)
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
        if (isMe) {
            val drawable: Drawable? =
                when (messageItem.status) {
                    MessageStatus.SENDING.name ->
                        AppCompatResources.getDrawable(itemView.context, R.drawable.ic_status_sending)
                    MessageStatus.SENT.name ->
                        AppCompatResources.getDrawable(itemView.context, R.drawable.ic_status_sent)
                    MessageStatus.DELIVERED.name ->
                        AppCompatResources.getDrawable(itemView.context, R.drawable.ic_status_delivered)
                    MessageStatus.READ.name ->
                        AppCompatResources.getDrawable(itemView.context, R.drawable.ic_status_read)
                    else -> null
                }
            drawable.also {
                it?.setBounds(0, 0, dp10, dp10)
                TextViewCompat.setCompoundDrawablesRelative(itemView.chat_time, null, null, drawable, null)
            }
        } else {
            TextViewCompat.setCompoundDrawablesRelative(itemView.chat_time, null, null, null, null)
        }
    }

    override fun chatLayout(isMe: Boolean, isLast: Boolean) {
        if (isMe) {
            (itemView.chat_layout.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.END
        } else {
            (itemView.chat_layout.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.START
        }
    }
}