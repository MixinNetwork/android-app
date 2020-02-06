package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import java.lang.Exception
import kotlinx.android.synthetic.main.item_chat_video.view.*
import one.mixin.android.R
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.fileSize
import one.mixin.android.extension.formatMillis
import one.mixin.android.extension.loadImageMark
import one.mixin.android.extension.loadVideoMark
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.round
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.isLive
import one.mixin.android.vo.isSignal
import org.jetbrains.anko.dip

class VideoHolder constructor(containerView: View) : MediaHolder(containerView) {

    init {
        val radius = itemView.context.dpToPx(4f).toFloat()
        itemView.chat_image.round(radius)
        itemView.chat_time.round(radius)
        itemView.progress.round(radius)
    }

    private val dp4 by lazy {
        itemView.context.dpToPx(4f)
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
            itemView.chat_name.visibility = VISIBLE
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
            itemView.chat_name.visibility = GONE
        }

        if (messageItem.isLive()) {
            itemView.chat_warning.visibility = GONE
            itemView.duration_tv.visibility = GONE
            itemView.progress.visibility = GONE
            itemView.play.isVisible = true
            itemView.live_tv.visibility = VISIBLE
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
        } else {
            itemView.live_tv.visibility = GONE
            if (messageItem.mediaStatus == MediaStatus.DONE.name) {
                messageItem.mediaDuration.notNullWithElse({
                    itemView.duration_tv.visibility = VISIBLE
                    itemView.duration_tv.text = try {
                        it.toLong().formatMillis()
                    } catch (e: Exception) {
                        ""
                    }
                }, {
                    itemView.duration_tv.visibility = GONE
                })
            } else {
                messageItem.mediaSize.notNullWithElse({
                    if (it == 0L) {
                        itemView.duration_tv.visibility = GONE
                    } else {
                        itemView.duration_tv.visibility = VISIBLE
                        itemView.duration_tv.text = it.fileSize()
                    }
                }, {
                    itemView.duration_tv.visibility = GONE
                })
            }
            messageItem.mediaStatus?.let {
                when (it) {
                    MediaStatus.EXPIRED.name -> {
                        itemView.chat_warning.visibility = VISIBLE
                        itemView.progress.visibility = GONE
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
        }
        itemView.chat_time.timeAgoClock(messageItem.createdAt)

        setStatusIcon(isMe, messageItem.status, messageItem.isSignal(), true) { statusIcon, secretIcon ->
            statusIcon?.setBounds(0, 0, dp12, dp12)
            secretIcon?.setBounds(0, 0, dp8, dp8)
            TextViewCompat.setCompoundDrawablesRelative(itemView.chat_time, secretIcon, null, statusIcon, null)
        }

        dataWidth = messageItem.mediaWidth
        dataHeight = messageItem.mediaHeight
        dataUrl = if (messageItem.isLive()) {
            messageItem.thumbUrl
        } else {
            messageItem.mediaUrl
        }
        type = messageItem.type
        dataThumbImage = messageItem.thumbImage
        chatLayout(isMe, isLast)
    }

    private var dataUrl: String? = null
    private var type: String? = null
    private var dataThumbImage: String? = null
    private var dataWidth: Int? = null
    private var dataHeight: Int? = null

    override fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean) {
        super.chatLayout(isMe, isLast, isBlink)
        if (isMe) {
            (itemView.chat_layout.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.END
            (itemView.chat_image_layout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 1f
            (itemView.duration_tv.layoutParams as ViewGroup.MarginLayoutParams).marginStart = dp4
            (itemView.live_tv.layoutParams as ViewGroup.MarginLayoutParams).marginStart = dp4
            (itemView.chat_time.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = dp10
        } else {
            (itemView.chat_layout.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.START
            (itemView.chat_image_layout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 0f
            (itemView.duration_tv.layoutParams as ViewGroup.MarginLayoutParams).marginStart = dp10
            (itemView.live_tv.layoutParams as ViewGroup.MarginLayoutParams).marginStart = dp10
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
            itemView.chat_image.layoutParams.height = width * dataHeight!! / dataWidth!!
        }

        val mark = when {
            isMe && isLast -> R.drawable.chat_mark_image_me
            isMe -> R.drawable.chat_mark_image
            !isMe && isLast -> R.drawable.chat_mark_image_other
            else -> R.drawable.chat_mark_image
        }

        itemView.chat_image.setShape(mark)
        if (type == MessageCategory.PLAIN_LIVE.name || type == MessageCategory.SIGNAL_LIVE.name) {
            itemView.chat_image.loadImageMark(dataUrl, R.drawable.image_holder, mark)
        } else {
            itemView.chat_image.loadVideoMark(dataUrl, dataThumbImage, mark)
        }
    }
}
