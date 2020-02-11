package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.view.Gravity
import android.view.View
import androidx.core.widget.TextViewCompat
import kotlinx.android.synthetic.main.item_chat_video_quote.view.*
import one.mixin.android.R
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.fileSize
import one.mixin.android.extension.formatMillis
import one.mixin.android.extension.loadVideo
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.realSize
import one.mixin.android.extension.round
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.QuoteMessageItem
import one.mixin.android.vo.isSignal
import org.jetbrains.anko.dip

class VideoQuoteHolder constructor(containerView: View) : BaseViewHolder(containerView) {
    private val dp16 = itemView.context.dpToPx(16f)
    private val minWidth by lazy {
        (itemView.context.realSize().x * 0.5).toInt()
    }

    init {
        val radius = itemView.context.dpToPx(4f).toFloat()
        itemView.chat_image_layout.round(radius)
        itemView.chat_time.round(radius)
    }

    override fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean) {
        super.chatLayout(isMe, isLast, isBlink)
        if (isMe) {
            itemView.chat_msg_layout.gravity = Gravity.END
            if (isLast) {
                setItemBackgroundResource(
                    itemView.chat_layout,
                    R.drawable.chat_bubble_reply_me_last,
                    R.drawable.chat_bubble_reply_me_last_night
                )
            } else {
                setItemBackgroundResource(
                    itemView.chat_layout,
                    R.drawable.chat_bubble_reply_me,
                    R.drawable.chat_bubble_reply_me_night
                )
            }
        } else {
            itemView.chat_msg_layout.gravity = Gravity.START

            if (isLast) {
                setItemBackgroundResource(
                    itemView.chat_layout,
                    R.drawable.chat_bubble_reply_other_last,
                    R.drawable.chat_bubble_reply_other_last_night
                )
            } else {
                setItemBackgroundResource(
                    itemView.chat_layout,
                    R.drawable.chat_bubble_reply_other,
                    R.drawable.chat_bubble_reply_other_night
                )
            }
        }
    }

    private var onItemListener: ConversationAdapter.OnItemListener? = null

    fun bind(
        messageItem: MessageItem,
        isLast: Boolean,
        isFirst: Boolean = false,
        hasSelect: Boolean,
        isSelect: Boolean,
        onItemListener: ConversationAdapter.OnItemListener
    ) {
        this.onItemListener = onItemListener
        if (hasSelect && isSelect) {
            itemView.setBackgroundColor(SELECT_COLOR)
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT)
        }

        itemView.chat_layout.chat_quote_layout.setRatio(messageItem.mediaWidth!!.toFloat() / messageItem.mediaHeight!!.toFloat())
        itemView.chat_layout.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(messageItem, adapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
                true
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

        itemView.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
            }
        }

        itemView.chat_time.timeAgoClock(messageItem.createdAt)
        if (messageItem.mediaStatus == MediaStatus.DONE.name) {
            messageItem.mediaDuration.notNullWithElse({
                itemView.duration_tv.visibility = View.VISIBLE
                itemView.duration_tv.text = try {
                    it.toLong().formatMillis()
                } catch (e: Exception) {
                    ""
                }
            }, {
                itemView.duration_tv.visibility = View.GONE
            })
        } else {
            messageItem.mediaSize.notNullWithElse({
                if (it == 0L) {
                    itemView.duration_tv.visibility = View.GONE
                } else {
                    itemView.duration_tv.visibility = View.VISIBLE
                    itemView.duration_tv.text = it.fileSize()
                }
            }, {
                itemView.duration_tv.visibility = View.GONE
            })
        }
        messageItem.mediaStatus?.let {
            when (it) {
                MediaStatus.EXPIRED.name -> {
                    itemView.chat_warning.visibility = View.VISIBLE
                    itemView.progress.visibility = View.GONE
                    itemView.play.visibility = View.GONE
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
                    itemView.play.visibility = View.GONE
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
                    itemView.play.visibility = View.VISIBLE
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
                    itemView.play.visibility = View.GONE
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

        itemView.chat_image.loadVideo(
            messageItem.mediaUrl,
            messageItem.thumbImage,
            minWidth,
            minWidth * messageItem.mediaHeight / messageItem.mediaWidth
        )

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
            itemView.chat_name.setTextColor(getColorById(messageItem.userId))
            itemView.chat_name.setOnClickListener { onItemListener.onUserClick(messageItem.userId) }
        } else {
            itemView.chat_name.visibility = View.GONE
        }

        if (messageItem.appId != null) {
            itemView.chat_name.setCompoundDrawables(null, null, botIcon, null)
            itemView.chat_name.compoundDrawablePadding = itemView.dip(3)
        } else {
            itemView.chat_name.setCompoundDrawables(null, null, null, null)
        }
        setStatusIcon(isMe, messageItem.status, messageItem.isSignal(), true) { statusIcon, secretIcon ->
            statusIcon?.setBounds(0, 0, dp12, dp12)
            secretIcon?.setBounds(0, 0, dp8, dp8)
            TextViewCompat.setCompoundDrawablesRelative(itemView.chat_time, secretIcon, null, statusIcon, null)
        }

        val quoteMessage = GsonHelper.customGson.fromJson(messageItem.quoteContent, QuoteMessageItem::class.java)
        itemView.chat_quote.bind(quoteMessage)
        itemView.chat_quote.setOnClickListener {
            if (!hasSelect) {
                onItemListener.onMessageClick(messageItem.quoteId)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
            }
        }
        chatLayout(isMe, isLast)
    }
}
