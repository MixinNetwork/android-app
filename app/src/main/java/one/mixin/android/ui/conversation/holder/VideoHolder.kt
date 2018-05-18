package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.support.constraint.ConstraintLayout
import android.support.v4.widget.TextViewCompat
import android.support.v7.content.res.AppCompatResources
import android.view.Gravity
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.item_chat_video.view.*
import one.mixin.android.R
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.formatMillis
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

class VideoHolder constructor(containerView: View) : BaseViewHolder(containerView) {

    init {
        val radius = itemView.context.dpToPx(4f).toFloat()
        itemView.chat_image.round(radius)
        itemView.chat_time.round(radius)
        itemView.progress.round(radius)
    }

    private val dp10 by lazy {
        itemView.context.dpToPx(10f)
    }

    private val dp94 by lazy {
        itemView.context.dpToPx(94f)
    }

    private val dp194 by lazy {
        itemView.context.dpToPx(194f)
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
            var maxWidth = dp194
            var minWidth = dp94
            when {
                messageItem.mediaWidth!! > maxWidth -> {
                    itemView.chat_image.layoutParams.width = maxWidth
                    itemView.chat_image.layoutParams.height =
                        maxWidth * messageItem.mediaHeight!! / messageItem.mediaWidth
                }
                messageItem.mediaWidth < minWidth -> {
                    itemView.chat_image.layoutParams.width = minWidth
                    itemView.chat_image.layoutParams.height =
                        minWidth * messageItem.mediaHeight!! / messageItem.mediaWidth
                }
                else -> {
                    itemView.chat_image.layoutParams.width = messageItem.mediaWidth
                    itemView.chat_image.layoutParams.height = messageItem.mediaHeight!!
                }
            }
        }
        val mark = R.drawable.chat_mark_image

        notNullElse(messageItem.mediaUrl, {
            itemView.chat_image.loadImageUseMark(it, R.drawable.image_holder, mark)
        }, {
            if (!isMe && messageItem.mediaWidth != 0 && messageItem.mediaHeight != 0) {
                if (thumbId != messageItem.thumbImage!!.hashCode()) {
                    itemView.chat_image.loadImage(messageItem.thumbImage.decodeBase64(),
                        itemView.chat_image.layoutParams.width, itemView.chat_image.layoutParams.height, mark)
                    thumbId = messageItem.thumbImage.hashCode()
                }
            }
        })
        notNullElse(messageItem.mediaDuration, {
            itemView.duration_tv.visibility = VISIBLE
            itemView.duration_tv.text = it.toLong().formatMillis()
        }, {
            itemView.duration_tv.visibility = GONE
        })

        itemView.chat_time.timeAgoClock(messageItem.createdAt)
        messageItem.mediaStatus?.let {
            when (it) {
                MediaStatus.EXPIRED.name -> {
                    itemView.chat_warning.visibility = View.VISIBLE
                    itemView.progress.visibility = View.GONE
                    itemView.play.visibility = GONE
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
                    itemView.chat_warning.visibility = GONE
                    itemView.progress.visibility = VISIBLE
                    itemView.play.visibility = GONE
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
                    itemView.chat_warning.visibility = GONE
                    itemView.progress.visibility = GONE
                    itemView.play.visibility = VISIBLE
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
                    itemView.chat_warning.visibility = GONE
                    itemView.progress.visibility = VISIBLE
                    itemView.play.visibility = GONE
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
        chatLayout(isMe, isLast)
    }

    override fun chatLayout(isMe: Boolean, isLast: Boolean) {
        if (isMe) {
            (itemView.chat_layout.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.END
            (itemView.chat_image_layout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 1f
        } else {
            (itemView.chat_layout.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.START
            (itemView.chat_image_layout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 0f
        }
    }
}