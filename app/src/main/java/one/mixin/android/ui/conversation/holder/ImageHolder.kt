package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.support.constraint.ConstraintLayout
import android.support.v4.widget.TextViewCompat
import android.support.v7.content.res.AppCompatResources
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.item_chat_image.view.*
import one.mixin.android.R
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.loadGif
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.loadImageUseMark
import one.mixin.android.extension.notNullElse
import one.mixin.android.extension.round
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.MessageStatus
import org.jetbrains.anko.dip

class ImageHolder constructor(containerView: View) : BaseViewHolder(containerView) {

    init {
        val radius = itemView.context.dpToPx(4f).toFloat()
        itemView.chat_image.round(radius)
        itemView.chat_time.round(radius)
        itemView.progress.round(radius)
    }

    private val dp10 by lazy {
        itemView.context.dpToPx(10f)
    }

    private val dp200 by lazy {
        itemView.context.dpToPx(200f)
    }

    private val dp94 by lazy {
        dp100 - dp6
    }

    private val dp100 by lazy {
        itemView.context.dpToPx(200f)
    }

    private val dp194 by lazy {
        dp200 - dp6
    }

    private val dp6 by lazy {
        itemView.context.dpToPx(6f)
    }

    private var thumbId: Int? = null

    fun bind(
        messageItem: MessageItem,
        isLast: Boolean,
        isFirst: Boolean,
        hasSelect: Boolean,
        isSelect: Boolean,
        onItemListener: ConversationAdapter.OnItemListener
    ) {
        if (hasSelect && isSelect) {
            itemView.setBackgroundColor(Color.parseColor("#660D94FC"))
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT)
        }
        val isGif = messageItem.mediaMineType.equals("image/gif", true)
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
            itemView.chat_name.setOnClickListener { onItemListener.onUserClick(messageItem.userId) }
            itemView.chat_name.setTextColor(colors[messageItem.userIdentityNumber.toLong().rem(colors.size).toInt()])
        } else {
            itemView.chat_name.visibility = View.GONE
        }
        if (messageItem.mediaWidth != 0 && messageItem.mediaHeight != 0) {
            var maxWith = dp194
            var minWitdh = dp94
            when {
                isLast && !isGif -> {
                    maxWith = dp200
                    minWitdh = dp100
                    (itemView.chat_image.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 0
                    (itemView.chat_image.layoutParams as ViewGroup.MarginLayoutParams).marginStart = 0
                }
                isMe -> {
                    (itemView.chat_image.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = dp6
                    (itemView.chat_image.layoutParams as ViewGroup.MarginLayoutParams).marginStart = 0
                }
                else -> {
                    (itemView.chat_image.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 0
                    (itemView.chat_image.layoutParams as ViewGroup.MarginLayoutParams).marginStart = dp6
                }
            }
            when {
                messageItem.mediaWidth!! > maxWith -> {
                    itemView.chat_image.layoutParams.width = maxWith
                    itemView.chat_image.layoutParams.height =
                        maxWith * messageItem.mediaHeight!! / messageItem.mediaWidth
                }
                messageItem.mediaWidth < minWitdh -> {
                    itemView.chat_image.layoutParams.width = minWitdh
                    itemView.chat_image.layoutParams.height =
                        minWitdh * messageItem.mediaHeight!! / messageItem.mediaWidth
                }
                else -> {
                    itemView.chat_image.layoutParams.width = messageItem.mediaWidth
                    itemView.chat_image.layoutParams.height = messageItem.mediaHeight!!
                }
            }
        }
        val mark = when {
            isMe && isLast -> R.drawable.chat_mark_image_me
            isMe -> R.drawable.chat_mark_image
            !isMe && isLast -> R.drawable.chat_mark_image_other
            else -> R.drawable.chat_mark_image
        }
        notNullElse(messageItem.mediaUrl, {
            if (isGif) {
                itemView.chat_image.loadGif(it)
            } else if (thumbId != messageItem.mediaUrl!!.hashCode() + mark) {
                itemView.chat_image.loadImageUseMark(it, R.drawable.image_holder, mark)
                thumbId = messageItem.mediaUrl.hashCode() + mark
            }
        }, {
            if (!isMe && messageItem.mediaWidth != 0 && messageItem.mediaHeight != 0) {
                if (thumbId != messageItem.thumbImage!!.hashCode() + mark) {
                    itemView.chat_image.loadImage(messageItem.thumbImage.decodeBase64(),
                        itemView.chat_image.layoutParams.width, itemView.chat_image.layoutParams.height, mark)
                    thumbId = messageItem.thumbImage.hashCode() + mark
                }
            }
        })
        itemView.chat_time.timeAgoClock(messageItem.createdAt)
        messageItem.mediaStatus?.let {
            when (it) {
                MediaStatus.EXPIRED.name -> {
                    itemView.chat_warning.visibility = View.VISIBLE
                    itemView.progress.visibility = View.GONE
                    itemView.chat_image.setOnLongClickListener {
                        if (!hasSelect) {
                            onItemListener.onLongClick(messageItem, adapterPosition)
                        } else {
                            true
                        }
                    }
                    itemView.chat_image.setOnClickListener {
                        if (hasSelect) {
                            onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
                        }
                    }
                }
                MediaStatus.PENDING.name -> {
                    itemView.chat_warning.visibility = View.GONE
                    itemView.progress.visibility = View.VISIBLE
                    itemView.progress.enableLoading()
                    itemView.progress.setBindId(messageItem.messageId)
                    itemView.progress.setOnLongClickListener {
                        if (!hasSelect) {
                            onItemListener.onLongClick(messageItem, adapterPosition)
                        } else {
                            false
                        }
                    }
                    itemView.progress.setOnClickListener {
                        if (hasSelect) {
                            onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
                        } else {
                            onItemListener.onCancel(messageItem.messageId)
                        }
                    }
                    itemView.chat_image.setOnClickListener { }
                    itemView.chat_image.setOnLongClickListener { false }
                }
                MediaStatus.DONE.name -> {
                    itemView.chat_warning.visibility = View.GONE
                    itemView.progress.visibility = View.GONE
                    itemView.progress.setBindId(messageItem.messageId)
                    itemView.progress.setOnClickListener {}
                    itemView.progress.setOnLongClickListener { false }
                    itemView.chat_image.setOnLongClickListener {
                        if (!hasSelect) {
                            onItemListener.onLongClick(messageItem, adapterPosition)
                        } else {
                            true
                        }
                    }
                    itemView.chat_image.setOnClickListener {
                        if (hasSelect) {
                            onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
                        } else {
                            onItemListener.onImageClick(messageItem, itemView.chat_image)
                        }
                    }
                }
                MediaStatus.CANCELED.name -> {
                    itemView.chat_warning.visibility = View.GONE
                    itemView.progress.visibility = View.VISIBLE
                    if (isMe) {
                        itemView.progress.enableUpload()
                    } else {
                        itemView.progress.enableDownload()
                    }
                    itemView.progress.setBindId(messageItem.messageId)
                    itemView.progress.setProgress(-1)
                    itemView.progress.setOnLongClickListener {
                        if (!hasSelect) {
                            onItemListener.onLongClick(messageItem, adapterPosition)
                        } else {
                            false
                        }
                    }
                    itemView.progress.setOnClickListener {
                        if (hasSelect) {
                            onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
                        } else {
                            if (isMe) {
                                onItemListener.onRetryUpload(messageItem.messageId)
                            } else {
                                onItemListener.onRetryDownload(messageItem.messageId)
                            }
                        }
                    }
                    itemView.chat_image.setOnClickListener {}
                    itemView.chat_image.setOnLongClickListener { false }
                }
            }
        }
        if (isMe) {
            val drawable: Drawable? =
                when (messageItem.status) {
                    MessageStatus.SENDING.name ->
                        AppCompatResources.getDrawable(itemView.context, R.drawable.ic_status_sending_white)
                    MessageStatus.SENT.name ->
                        AppCompatResources.getDrawable(itemView.context, R.drawable.ic_status_sent_white)
                    MessageStatus.DELIVERED.name ->
                        AppCompatResources.getDrawable(itemView.context, R.drawable.ic_status_delivered_white)
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
        if (isGif) {
            chatLayout(isMe, false)
        } else {
            chatLayout(isMe, isLast)
        }
    }

    override fun chatLayout(isMe: Boolean, isLast: Boolean) {
        if (isMe) {
            if (isLast) {
                itemView.chat_time.setBackgroundResource(R.drawable.chat_bubble_shadow_last)
            } else {
                itemView.chat_time.setBackgroundResource(R.drawable.chat_bubble_shadow)
            }
            (itemView.chat_layout.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.END
            (itemView.chat_image_layout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 1f
        } else {
            if (isLast) {
                itemView.chat_time.setBackgroundResource(R.drawable.chat_bubble_shadow)
            } else {
                itemView.chat_time.setBackgroundResource(R.drawable.chat_bubble_shadow)
            }
            (itemView.chat_layout.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.START
            (itemView.chat_image_layout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 0f
        }
    }
}