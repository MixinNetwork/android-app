package one.mixin.android.ui.conversation.chathistory.holder

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatVideoQuoteBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.fileSize
import one.mixin.android.extension.formatMillis
import one.mixin.android.extension.loadVideo
import one.mixin.android.extension.realSize
import one.mixin.android.extension.round
import one.mixin.android.job.MixinJobManager
import one.mixin.android.session.Session
import one.mixin.android.ui.conversation.chathistory.ChatHistoryAdapter
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.ChatHistoryMessageItem
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.QuoteMessageItem
import one.mixin.android.vo.absolutePath

class VideoQuoteHolder constructor(val binding: ItemChatVideoQuoteBinding) : BaseViewHolder(binding.root) {
    private val minWidth by lazy {
        (itemView.context.realSize().x * 0.5).toInt()
    }

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
        val isMe = messageItem.userId == Session.getAccountId()
        binding.chatQuoteLayout.setRatio(messageItem.mediaWidth!!.toFloat() / messageItem.mediaHeight!!.toFloat())

        when (messageItem.mediaStatus) {
            MediaStatus.DONE.name -> {
                binding.durationTv.bindId(null)
                val mediaDuration = messageItem.mediaDuration
                if (mediaDuration != null) {
                    binding.durationTv.visibility = View.VISIBLE
                    binding.durationTv.text = mediaDuration.toLongOrNull()?.formatMillis() ?: ""
                } else {
                    binding.durationTv.visibility = View.GONE
                }
            }
            MediaStatus.PENDING.name -> {
                val mediaSize = messageItem.mediaSize
                if (mediaSize != null) {
                    binding.durationTv.visibility = View.VISIBLE
                    if (mediaSize == 0L) {
                        binding.durationTv.bindId(messageItem.messageId)
                    } else {
                        binding.durationTv.text = mediaSize.fileSize()
                        binding.durationTv.bindId(null)
                    }
                } else {
                    binding.durationTv.bindId(null)
                    binding.durationTv.visibility = View.GONE
                }
            }
            else -> {
                val mediaSize = messageItem.mediaSize
                if (mediaSize != null) {
                    if (mediaSize == 0L) {
                        binding.durationTv.visibility = View.GONE
                    } else {
                        binding.durationTv.visibility = View.VISIBLE
                        binding.durationTv.text = mediaSize.fileSize()
                    }
                } else {
                    binding.durationTv.visibility = View.GONE
                }
                binding.durationTv.bindId(null)
            }
        }
        messageItem.mediaStatus?.let {
            when (it) {
                MediaStatus.EXPIRED.name -> {
                    binding.chatWarning.visibility = View.VISIBLE
                    binding.progress.visibility = View.GONE
                    binding.play.visibility = View.GONE
                }
                MediaStatus.PENDING.name -> {
                    binding.chatWarning.visibility = View.GONE
                    binding.progress.visibility = View.VISIBLE
                    binding.play.visibility = View.GONE
                    binding.progress.enableLoading(MixinJobManager.getAttachmentProcess(messageItem.messageId))
                    binding.progress.setBindOnly("${messageItem.transcriptId ?: ""}${messageItem.messageId}")
                    binding.progress.setOnClickListener {
                        onItemListener.onCancel(messageItem.transcriptId, messageItem.messageId)
                    }
                    binding.chatImage.setOnClickListener { }
                }
                MediaStatus.DONE.name -> {
                    binding.chatWarning.visibility = View.GONE
                    binding.progress.visibility = View.GONE
                    binding.play.visibility = View.VISIBLE
                    binding.progress.setBindId("${messageItem.transcriptId ?: ""}${messageItem.messageId}")
                    binding.progress.setOnClickListener {}
                    binding.chatImage.setOnClickListener {
                        onItemListener.onImageClick(messageItem, binding.chatImage)
                    }
                }
                MediaStatus.CANCELED.name -> {
                    binding.play.visibility = View.GONE
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

        binding.chatImage.loadVideo(
            messageItem.absolutePath(),
            messageItem.thumbImage,
            minWidth,
            minWidth * messageItem.mediaHeight / messageItem.mediaWidth,
        )

        if (isFirst && !isMe) {
            binding.chatName.visibility = View.VISIBLE
            binding.chatName.text = messageItem.userFullName
            if (messageItem.membership != null) {
                binding.chatName.setCompoundDrawables(null, null, getMembershipBadge(messageItem), null)
                binding.chatName.compoundDrawablePadding = 3.dp
            } else if (messageItem.appId != null) {
                binding.chatName.setCompoundDrawables(null, null, botIcon, null)
                binding.chatName.compoundDrawablePadding = 3.dp
            } else {
                binding.chatName.setCompoundDrawables(null, null, null, null)
            }
            binding.chatName.setTextColor(getColorById(messageItem.userId))
            binding.chatName.setOnClickListener { onItemListener.onUserClick(messageItem.userId) }
        } else {
            binding.chatName.visibility = View.GONE
        }

        if (messageItem.membership != null) {
            binding.chatName.setCompoundDrawables(null, null, getMembershipBadge(messageItem), null)
            binding.chatName.compoundDrawablePadding = 3.dp
        } else if (messageItem.appId != null) {
            binding.chatName.setCompoundDrawables(null, null, botIcon, null)
            binding.chatName.compoundDrawablePadding = 3.dp
        } else {
            binding.chatName.setCompoundDrawables(null, null, null, null)
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

        binding.chatQuote.bind(GsonHelper.customGson.fromJson(messageItem.quoteContent, QuoteMessageItem::class.java))

        binding.chatQuote.setOnClickListener {
            onItemListener.onQuoteMessageClick(messageItem.messageId, messageItem.quoteId)
        }
        chatLayout(isMe, isLast)
        if (messageItem.transcriptId == null) {
            binding.root.setOnLongClickListener {
                onItemListener.onMenu(binding.chatJump, messageItem)
                true
            }
            binding.chatMsgLayout.setOnLongClickListener {
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
