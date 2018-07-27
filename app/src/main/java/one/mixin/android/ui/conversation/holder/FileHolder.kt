package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.date_wrapper.view.*
import kotlinx.android.synthetic.main.item_chat_file.view.*
import one.mixin.android.R
import one.mixin.android.extension.fileSize
import one.mixin.android.extension.notNullElse
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageItem
import org.jetbrains.anko.dip
import org.jetbrains.anko.textResource

class FileHolder constructor(containerView: View) : BaseViewHolder(containerView) {
    init {
        itemView.chat_flag.visibility = View.GONE
    }

    fun bind(
        messageItem: MessageItem,
        keyword: String?,
        isFirst: Boolean,
        isLast: Boolean,
        hasSelect: Boolean,
        isSelect: Boolean,
        onItemListener: ConversationAdapter.OnItemListener
    ) {
        if (hasSelect && isSelect) {
            itemView.setBackgroundColor(SELECT_COLOR)
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT)
        }

        val isMe = meId == messageItem.userId
        chatLayout(isMe, isLast)
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
        itemView.chat_time.timeAgoClock(messageItem.createdAt)
        notNullElse(keyword, { k ->
            messageItem.mediaName?.let { str ->
                val start = str.indexOf(k, 0, true)
                if (start >= 0) {
                    val sp = SpannableString(str)
                    sp.setSpan(BackgroundColorSpan(HIGHLIGHTED), start,
                        start + k.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    itemView.file_name_tv.text = sp
                } else {
                    itemView.file_name_tv.text = messageItem.mediaName
                }
            }
        }, {
            itemView.file_name_tv.text = messageItem.mediaName
        })
        if (messageItem.mediaStatus == MediaStatus.EXPIRED.name) {
            itemView.file_size_tv.textResource = R.string.chat_expired
        } else {
            itemView.file_size_tv.text = "${messageItem.mediaSize?.fileSize()}"
        }
        setStatusIcon(isMe, messageItem.status, {
            itemView.chat_flag.setImageDrawable(it)
            itemView.chat_flag.visibility = View.VISIBLE
        }, {
            itemView.chat_flag.visibility = View.GONE
        })
        messageItem.mediaStatus?.let {
            when (it) {
                MediaStatus.EXPIRED.name -> {
                    itemView.file_expired.visibility = View.VISIBLE
                    itemView.file_progress.visibility = View.INVISIBLE
                    itemView.setOnClickListener {
                        handlerClick(hasSelect, isSelect, isMe, messageItem, onItemListener)
                    }
                }
                MediaStatus.PENDING.name -> {
                    itemView.file_expired.visibility = View.GONE
                    itemView.file_progress.visibility = View.VISIBLE
                    itemView.file_progress.enableLoading()
                    itemView.file_progress.setBindId(messageItem.messageId)
                    itemView.file_progress.setOnClickListener {
                        onItemListener.onCancel(messageItem.messageId)
                    }
                    itemView.setOnClickListener {
                        handlerClick(hasSelect, isSelect, isMe, messageItem, onItemListener)
                    }
                }
                MediaStatus.DONE.name -> {
                    itemView.file_expired.visibility = View.GONE
                    itemView.file_progress.visibility = View.VISIBLE
                    itemView.file_progress.setDone()
                    itemView.file_progress.setBindId(null)
                    itemView.file_progress.setOnClickListener {
                        handlerClick(hasSelect, isSelect, isMe, messageItem, onItemListener)
                    }
                    itemView.setOnClickListener {
                        handlerClick(hasSelect, isSelect, isMe, messageItem, onItemListener)
                    }
                }
                MediaStatus.CANCELED.name -> {
                    itemView.file_expired.visibility = View.GONE
                    itemView.file_progress.visibility = View.VISIBLE
                    if (isMe) {
                        itemView.file_progress.enableUpload()
                    } else {
                        itemView.file_progress.enableDownload()
                    }
                    itemView.file_progress.setBindId(messageItem.messageId)
                    itemView.file_progress.setProgress(-1)
                    itemView.file_progress.setOnClickListener {
                        if (isMe) {
                            onItemListener.onRetryUpload(messageItem.messageId)
                        } else {
                            onItemListener.onRetryDownload(messageItem.messageId)
                        }
                    }
                    itemView.setOnClickListener {
                        handlerClick(hasSelect, isSelect, isMe, messageItem, onItemListener)
                    }
                }
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
    }

    private fun handlerClick(
        hasSelect: Boolean,
        isSelect: Boolean,
        isMe: Boolean,
        messageItem: MessageItem,
        onItemListener: ConversationAdapter.OnItemListener
    ) {
        if (hasSelect) {
            onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
        } else if (messageItem.mediaStatus == MediaStatus.CANCELED.name) {
            if (isMe) {
                onItemListener.onRetryUpload(messageItem.messageId)
            } else {
                onItemListener.onRetryDownload(messageItem.messageId)
            }
        } else if (messageItem.mediaStatus == MediaStatus.PENDING.name) {
            onItemListener.onCancel(messageItem.messageId)
        } else if (messageItem.mediaStatus == MediaStatus.EXPIRED.name) {
        } else {
            onItemListener.onFileClick(messageItem)
        }
    }

    override fun chatLayout(isMe: Boolean, isLast: Boolean) {
        if (isMe) {
            if (isLast) {
                itemView.chat_layout.setBackgroundResource(R.drawable.bill_bubble_me_last)
            } else {
                itemView.chat_layout.setBackgroundResource(R.drawable.bill_bubble_me)
            }
            (itemView.chat_layout.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.END
        } else {
            (itemView.chat_layout.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.START
            if (isLast) {
                itemView.chat_layout.setBackgroundResource(R.drawable.chat_bubble_other_last)
            } else {
                itemView.chat_layout.setBackgroundResource(R.drawable.chat_bubble_other)
            }
        }
    }
}