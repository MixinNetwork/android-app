package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.FrameLayout
import androidx.core.widget.TextViewCompat
import kotlinx.android.synthetic.main.item_chat_sticker.view.*
import one.mixin.android.R
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.loadSticker
import one.mixin.android.extension.round
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.util.image.ImageListener
import one.mixin.android.util.image.LottieLoader
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.isLottieUrl
import one.mixin.android.vo.isSignal
import one.mixin.android.widget.RLottieDrawable
import org.jetbrains.anko.dip
import org.jetbrains.anko.textColorResource

class StickerHolder constructor(containerView: View) : BaseViewHolder(containerView) {

    init {
        val radius = itemView.context.dpToPx(4f).toFloat()
        itemView.chat_time.textColorResource = R.color.color_chat_date
        itemView.chat_sticker.round(radius)
    }

    private val dp120 by lazy {
        itemView.context.dpToPx(120f)
    }

    private val dp48 by lazy {
        itemView.context.dpToPx(48f)
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
                onItemListener.onLongClick(messageItem, absoluteAdapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
                true
            }
        }
        itemView.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
            }
        }
        if (messageItem.assetWidth == null || messageItem.assetHeight == null) {
            itemView.chat_sticker.layoutParams.width = dp120
            itemView.chat_sticker.layoutParams.height = dp120
            itemView.chat_time.visibility = INVISIBLE
        } else if (messageItem.assetWidth * 2 < dp48 || messageItem.assetHeight * 2 < dp48) {
            if (messageItem.assetWidth < messageItem.assetHeight) {
                if (dp48 * messageItem.assetHeight / messageItem.assetWidth > dp120) {
                    itemView.chat_sticker.layoutParams.width = dp120 * messageItem.assetWidth / messageItem.assetHeight
                    itemView.chat_sticker.layoutParams.height = dp120
                } else {
                    itemView.chat_sticker.layoutParams.width = dp48
                    itemView.chat_sticker.layoutParams.height = dp48 * messageItem.assetHeight / messageItem.assetWidth
                }
            } else {
                if (dp48 * messageItem.assetWidth / messageItem.assetHeight > dp120) {
                    itemView.chat_sticker.layoutParams.height = dp120 * messageItem.assetHeight / messageItem.assetWidth
                    itemView.chat_sticker.layoutParams.width = dp120
                } else {
                    itemView.chat_sticker.layoutParams.height = dp48
                    itemView.chat_sticker.layoutParams.width = dp48 * messageItem.assetWidth / messageItem.assetHeight
                }
            }
            itemView.chat_time.visibility = VISIBLE
        } else if (messageItem.assetWidth * 2 > dp120 || messageItem.assetHeight * 2 > dp120) {
            if (messageItem.assetWidth > messageItem.assetHeight) {
                itemView.chat_sticker.layoutParams.width = dp120
                itemView.chat_sticker.layoutParams.height = dp120 * messageItem.assetHeight / messageItem.assetWidth
            } else {
                itemView.chat_sticker.layoutParams.height = dp120
                itemView.chat_sticker.layoutParams.width = dp120 * messageItem.assetWidth / messageItem.assetHeight
            }
            itemView.chat_time.visibility = VISIBLE
        } else {
            itemView.chat_sticker.layoutParams.width = messageItem.assetWidth * 2
            itemView.chat_sticker.layoutParams.height = messageItem.assetHeight * 2
            itemView.chat_time.visibility = VISIBLE
        }
        messageItem.assetUrl?.let { url ->
            if (url.isLottieUrl()) {
                LottieLoader.fromUrl(
                    itemView.context, url, url,
                    itemView.chat_sticker.layoutParams.width, itemView.chat_sticker.layoutParams.height
                )
                    .addListener(object : ImageListener<RLottieDrawable> {
                        override fun onResult(result: RLottieDrawable) {
                            itemView.chat_sticker.setAnimation(result)
                            itemView.chat_sticker.playAnimation()
                            itemView.chat_sticker.setAutoRepeat(true)
                        }
                    })
            } else {
                itemView.chat_sticker.loadSticker(url, messageItem.assetType)
            }
        }
        itemView.chat_time.timeAgoClock(messageItem.createdAt)
        if (isFirst && !isMe) {
            itemView.chat_name.visibility = VISIBLE
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
            itemView.chat_name.visibility = GONE
        }
        setStatusIcon(isMe, messageItem.status, messageItem.isSignal()) { statusIcon, secretIcon ->
            statusIcon?.setBounds(0, 0, dp12, dp12)
            secretIcon?.setBounds(0, 0, dp8, dp8)
            TextViewCompat.setCompoundDrawablesRelative(itemView.chat_time, secretIcon, null, statusIcon, null)
        }
        chatLayout(isMe, false)
    }

    override fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean) {
        super.chatLayout(isMe, isLast, isBlink)
        if (isMe) {
            (itemView.chat_layout.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.END
            itemView.requestLayout()
        } else {
            (itemView.chat_layout.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.START
            itemView.requestLayout()
        }
    }
}
