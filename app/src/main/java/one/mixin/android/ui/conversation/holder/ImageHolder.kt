package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.TextViewCompat
import kotlin.math.min
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
import one.mixin.android.vo.isSignal
import one.mixin.android.widget.gallery.MimeType
import org.jetbrains.anko.dip

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
            itemView.chat_name.setTextColor(getColorById(messageItem.userId))
        } else {
            itemView.chat_name.visibility = View.GONE
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
                    if (isMe && messageItem.mediaUrl != null) {
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
                            if (isMe && messageItem.mediaUrl != null) {
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
        setStatusIcon(isMe, messageItem.status, messageItem.isSignal(), true) { statusIcon, secretIcon ->
            statusIcon?.setBounds(0, 0, dp12, dp12)
            secretIcon?.setBounds(0, 0, dp8, dp8)
            TextViewCompat.setCompoundDrawablesRelative(itemView.chat_time, secretIcon, null, statusIcon, null)
        }

        dataWidth = messageItem.mediaWidth
        dataHeight = messageItem.mediaHeight
        dataUrl = messageItem.mediaUrl
        dataThumbImage = messageItem.thumbImage
        dataSize = messageItem.mediaSize
        isGif = messageItem.mediaMimeType.equals(MimeType.GIF.toString(), true)
        chatLayout(isMe, isLast)
    }

    private var isGif = false
    private var dataUrl: String? = null
    private var dataThumbImage: String? = null
    private var dataWidth: Int? = null
    private var dataHeight: Int? = null
    private var dataSize: Long? = null

    override fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean) {
        super.chatLayout(isMe, isLast, isBlink)
        if (isMe) {
            (itemView.chat_layout.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.END
            (itemView.chat_image_layout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 1f
            (itemView.chat_time.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = dp10
        } else {
            (itemView.chat_layout.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.START
            (itemView.chat_image_layout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 0f
            (itemView.chat_time.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = dp3
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
        if (dataWidth == null || dataHeight == null ||
            dataWidth!! <= 0 || dataHeight!! <= 0) {
            itemView.chat_image.layoutParams.width = width
            itemView.chat_image.layoutParams.height = width
        } else {
            itemView.chat_image.layoutParams.width = width
            itemView.chat_image.layoutParams.height =
                min(width * dataHeight!! / dataWidth!!, mediaHeight)
        }
        val mark = when {
            isMe && isLast -> R.drawable.chat_mark_image_me
            isMe -> R.drawable.chat_mark_image
            !isMe && isLast -> R.drawable.chat_mark_image_other
            else -> R.drawable.chat_mark_image
        }

        itemView.chat_image.setShape(mark)
        if (isBlink) {
            when {
                isGif -> handleGif(mark)
                itemView.chat_image.layoutParams.height == mediaHeight -> itemView.chat_image.loadLongImageMark(dataUrl, mark)
                else -> itemView.chat_image.loadImageMark(dataUrl, mark)
            }
        } else {
            when {
                isGif -> handleGif(mark)
                itemView.chat_image.layoutParams.height == mediaHeight -> itemView.chat_image.loadLongImageMark(dataUrl, dataThumbImage, mark)
                else -> itemView.chat_image.loadImageMark(dataUrl, dataThumbImage, mark)
            }
        }
    }

    private fun handleGif(mark: Int) {
        if (dataSize == null || dataSize == 0L) { // un-downloaded giphy
            itemView.chat_image.loadGifMark(dataThumbImage, mark, false)
        } else {
            itemView.chat_image.loadGifMark(dataUrl, dataThumbImage, mark)
        }
    }
}
