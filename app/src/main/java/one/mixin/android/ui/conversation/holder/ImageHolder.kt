package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.support.constraint.ConstraintLayout
import android.support.v4.widget.TextViewCompat
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.item_chat_image.view.*
import one.mixin.android.R
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.loadGifMark
import one.mixin.android.extension.loadImageMark
import one.mixin.android.extension.loadLongImageMark
import one.mixin.android.extension.round
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageItem
import one.mixin.android.widget.gallery.MimeType
import org.jetbrains.anko.dip
import kotlin.math.min

class ImageHolder constructor(containerView: View) : MediaHolder(containerView) {

    init {
        val radius = itemView.context.dpToPx(4f).toFloat()
        itemView.chat_image.round(radius)
        itemView.chat_time.round(radius)
        itemView.progress.round(radius)
    }

    fun bind(
        messageItem: MessageItem,
        isLast: Boolean,
        isFirst: Boolean,
        hasSelect: Boolean,
        isSelect: Boolean,
        onItemListener: ConversationAdapter.OnItemListener
    ) {
        listen(messageItem.messageId)
        if (hasSelect && isSelect) {
            itemView.setBackgroundColor(SELECT_COLOR)
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT)
        }
        val isGif = messageItem.mediaMimeType.equals(MimeType.GIF.toString(), true)
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

        var width = mediaWidth - dp6
        when {
            isLast -> {
                width = mediaWidth
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
        if (messageItem.mediaWidth == null || messageItem.mediaHeight == null ||
            messageItem.mediaWidth <= 0 || messageItem.mediaHeight <= 0) {
            itemView.chat_image.layoutParams.width = width
            itemView.chat_image.layoutParams.height = width
        } else {
            itemView.chat_image.layoutParams.width = width
            itemView.chat_image.layoutParams.height =
                min(width * messageItem.mediaHeight / messageItem.mediaWidth, mediaHeight)
        }

        val mark = when {
            isMe && isLast -> R.drawable.chat_mark_image_me
            isMe -> R.drawable.chat_mark_image
            !isMe && isLast -> R.drawable.chat_mark_image_other
            else -> R.drawable.chat_mark_image
        }

        itemView.chat_image.setShape(mark)
        if (isGif) {
            itemView.chat_image.loadGifMark(messageItem.mediaUrl, messageItem.thumbImage, mark)
        } else if (itemView.chat_image.layoutParams.height == mediaHeight) {
            itemView.chat_image.loadLongImageMark(messageItem.mediaUrl, messageItem.thumbImage, mark)
        } else {
            itemView.chat_image.loadImageMark(messageItem.mediaUrl, messageItem.thumbImage, mark)
        }

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
        setStatusIcon(isMe, messageItem.status, {
            TextViewCompat.setCompoundDrawablesRelative(itemView.chat_time, null, null, it, null)
        }, {
            TextViewCompat.setCompoundDrawablesRelative(itemView.chat_time, null, null, null, null)
        }, true)
        chatLayout(isMe, isLast)
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