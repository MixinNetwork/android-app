package one.mixin.android.ui.conversation.chathistory.holder

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatImageQuoteBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.loadLongImageMark
import one.mixin.android.extension.round
import one.mixin.android.job.MixinJobManager.Companion.getAttachmentProcess
import one.mixin.android.session.Session
import one.mixin.android.ui.conversation.chathistory.ChatHistoryAdapter
import one.mixin.android.vo.ChatHistoryMessageItem
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.absolutePath
import kotlin.math.min

class ImageQuoteHolder constructor(val binding: ItemChatImageQuoteBinding) : MediaHolder(binding.root) {
    private val dp16 = itemView.context.dpToPx(16f)

    init {
        val radius = itemView.context.dpToPx(4f).toFloat()
        binding.chatImageLayout.round(radius)
        binding.chatTime.round(radius)
    }

    override fun chatLayout(
        isMe: Boolean,
        isLast: Boolean,
        isBlink: Boolean,
    ) {
        super.chatLayout(isMe, isLast, isBlink)
        if (isMe) {
            (binding.chatMsgLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 1f
            if (isLast) {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_reply_me_last,
                    R.drawable.chat_bubble_reply_me_last_night,
                )
            } else {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_reply_me,
                    R.drawable.chat_bubble_reply_me_night,
                )
            }
        } else {
            (binding.chatMsgLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 0f
            if (isLast) {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_reply_other_last,
                    R.drawable.chat_bubble_reply_other_last_night,
                )
            } else {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_reply_other,
                    R.drawable.chat_bubble_reply_other_night,
                )
            }
        }
    }

    private var onItemListener: ChatHistoryAdapter.OnItemListener? = null

    fun bind(
        messageItem: ChatHistoryMessageItem,
        isLast: Boolean,
        isFirst: Boolean = false,
        onItemListener: ChatHistoryAdapter.OnItemListener,
    ) {
        super.bind(messageItem)
        this.onItemListener = onItemListener
        binding.chatQuoteLayout.setRatio(messageItem.mediaWidth!!.toFloat() / messageItem.mediaHeight!!.toFloat())

        messageItem.mediaStatus?.let {
            when (it) {
                MediaStatus.EXPIRED.name -> {
                    binding.chatWarning.visibility = View.VISIBLE
                    binding.progress.visibility = View.GONE
                }
                MediaStatus.PENDING.name -> {
                    binding.chatWarning.visibility = View.GONE
                    binding.progress.visibility = View.VISIBLE
                    binding.progress.enableLoading(getAttachmentProcess(messageItem.messageId))
                    binding.progress.setBindOnly("${messageItem.transcriptId ?: ""}${messageItem.messageId}")
                    binding.progress.setOnClickListener {
                        onItemListener.onCancel(messageItem.transcriptId, messageItem.messageId)
                    }
                    binding.chatImage.setOnClickListener { }
                }
                MediaStatus.DONE.name -> {
                    binding.chatWarning.visibility = View.GONE
                    binding.progress.visibility = View.GONE
                    binding.progress.setBindId("${messageItem.transcriptId ?: ""}${messageItem.messageId}")
                    binding.chatImage.setOnClickListener {
                        onItemListener.onImageClick(messageItem, binding.chatImage)
                    }
                }
                MediaStatus.CANCELED.name -> {
                    binding.chatWarning.visibility = View.GONE
                    binding.progress.visibility = View.VISIBLE
                    if (messageItem.transcriptId != null && messageItem.mediaUrl != null) {
                        binding.progress.enableUpload()
                    } else if (messageItem.mediaUrl != null && isMe) {
                        binding.progress.enableUpload()
                    } else {
                        binding.progress.enableDownload()
                    }
                    binding.progress.setBindId("${messageItem.transcriptId ?: ""}${messageItem.messageId}")
                    binding.progress.setProgress(-1)

                    binding.progress.setOnClickListener {
                        if (messageItem.mediaUrl.isNullOrEmpty()) {
                            onItemListener.onRetryDownload(messageItem.transcriptId, messageItem.messageId)
                        } else {
                            onItemListener.onRetryUpload(messageItem.transcriptId, messageItem.messageId)
                        }
                    }
                    binding.chatImage.setOnClickListener {}
                }
            }
        }

        val dataWidth = messageItem.mediaWidth
        val dataHeight = messageItem.mediaHeight
        val width = mediaWidth - dp6
        binding.chatImageLayout.layoutParams.width = width
        if (dataWidth <= 0 || dataHeight <= 0) {
            binding.chatImage.layoutParams.width = width
            binding.chatImage.layoutParams.height = width
        } else {
            binding.chatImage.layoutParams.width = width
            binding.chatImage.layoutParams.height =
                min(width * dataHeight / dataWidth, mediaHeight)
        }
        binding.chatImage.loadLongImageMark(messageItem.absolutePath(), null)

        val isMe = messageItem.userId == Session.getAccountId()
        if (isFirst && !isMe) {
            binding.chatName.visibility = View.VISIBLE
            binding.chatName.setMessageName(messageItem)
            binding.chatName.setTextColor(getColorById(messageItem.userId))
            binding.chatName.setOnClickListener { onItemListener.onUserClick(messageItem.userId) }
        } else {
            binding.chatName.visibility = View.GONE
        }

        binding.chatTime.load(
            isMe,
            messageItem.createdAt,
            MessageStatus.DELIVERED.name,
            false,
            isRepresentative = false,
            isSecret = false,
            isWhite = true,
        )

        binding.chatQuote.bind(fromJsonQuoteMessage(messageItem.quoteContent))

        binding.chatQuote.setOnClickListener {
            onItemListener.onQuoteMessageClick(messageItem.messageId, messageItem.quoteId)
        }

        chatLayout(isMe, isLast)
        if (messageItem.transcriptId == null) {
            binding.root.setOnLongClickListener {
                onItemListener.onMenu(binding.chatJump, messageItem)
                true
            }
            binding.chatImage.setOnLongClickListener {
                onItemListener.onMenu(binding.chatJump, messageItem)
                true
            }
            chatJumpLayout(binding.chatJump, isMe, messageItem.messageId, R.id.chat_msg_layout, onItemListener)
        }
    }
}
