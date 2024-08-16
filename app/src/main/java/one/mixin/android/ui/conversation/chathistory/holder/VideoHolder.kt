package one.mixin.android.ui.conversation.chathistory.holder

import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatVideoBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.fileSize
import one.mixin.android.extension.formatMillis
import one.mixin.android.extension.loadImageMark
import one.mixin.android.extension.loadVideoMark
import one.mixin.android.extension.round
import one.mixin.android.job.MixinJobManager
import one.mixin.android.session.Session
import one.mixin.android.ui.conversation.chathistory.ChatHistoryAdapter
import one.mixin.android.vo.ChatHistoryMessageItem
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.absolutePath
import one.mixin.android.vo.isLive
import one.mixin.android.vo.isSecret

class VideoHolder constructor(val binding: ItemChatVideoBinding) : MediaHolder(binding.root) {
    init {
        val radius = itemView.context.dpToPx(4f).toFloat()
        binding.chatImage.round(radius)
        binding.chatTime.round(radius)
        binding.progress.round(radius)
    }

    private val dp4 by lazy {
        itemView.context.dpToPx(4f)
    }

    fun bind(
        messageItem: ChatHistoryMessageItem,
        isLast: Boolean,
        isFirst: Boolean,
        onItemListener: ChatHistoryAdapter.OnItemListener,
    ) {
        super.bind(messageItem)
        val isMe = messageItem.userId == Session.getAccountId()
        if (isFirst && !isMe) {
            binding.chatName.visibility = VISIBLE
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
            binding.chatName.setOnClickListener { onItemListener.onUserClick(messageItem.userId) }
            binding.chatName.setTextColor(getColorById(messageItem.userId))
        } else {
            binding.chatName.visibility = GONE
        }

        if (messageItem.isLive()) {
            binding.chatWarning.visibility = GONE
            binding.durationTv.visibility = GONE
            binding.progress.visibility = GONE
            binding.play.isVisible = true
            binding.liveTv.visibility = VISIBLE
            binding.progress.setBindId("${messageItem.transcriptId ?: ""}${messageItem.messageId}")
            binding.progress.setOnClickListener {}
            binding.progress.setOnLongClickListener { false }
            binding.chatImage.setOnClickListener {
                onItemListener.onImageClick(messageItem, binding.chatImage)
            }
        } else {
            binding.liveTv.visibility = GONE
            when (messageItem.mediaStatus) {
                MediaStatus.DONE.name -> {
                    binding.durationTv.bindId(null)
                    val mediaDuration = messageItem.mediaDuration
                    if (mediaDuration != null) {
                        binding.durationTv.visibility = VISIBLE
                        binding.durationTv.text = mediaDuration.toLongOrNull()?.formatMillis() ?: ""
                    } else {
                        binding.durationTv.visibility = GONE
                    }
                }
                MediaStatus.PENDING.name -> {
                    val mediaSize = messageItem.mediaSize
                    if (mediaSize != null) {
                        binding.durationTv.visibility = VISIBLE
                        if (mediaSize == 0L) {
                            binding.durationTv.bindId(messageItem.messageId)
                        } else {
                            binding.durationTv.text = mediaSize.fileSize()
                            binding.durationTv.bindId(null)
                        }
                    } else {
                        binding.durationTv.bindId(null)
                        binding.durationTv.visibility = GONE
                    }
                }
                else -> {
                    val mediaSize = messageItem.mediaSize
                    if (mediaSize != null) {
                        if (mediaSize == 0L) {
                            binding.durationTv.visibility = GONE
                        } else {
                            binding.durationTv.visibility = VISIBLE
                            binding.durationTv.text = mediaSize.fileSize()
                        }
                    } else {
                        binding.durationTv.visibility = GONE
                    }
                    binding.durationTv.bindId(null)
                }
            }
            messageItem.mediaStatus?.let {
                when (it) {
                    MediaStatus.EXPIRED.name -> {
                        binding.chatWarning.visibility = VISIBLE
                        binding.progress.visibility = GONE
                        binding.play.visibility = GONE
                        binding.chatImage.setOnClickListener {
                        }
                    }
                    MediaStatus.PENDING.name -> {
                        binding.chatWarning.visibility = GONE
                        binding.progress.visibility = VISIBLE
                        binding.play.visibility = GONE
                        binding.progress.enableLoading(MixinJobManager.getAttachmentProcess(messageItem.messageId))
                        binding.progress.setBindOnly("${messageItem.transcriptId ?: ""}${messageItem.messageId}")
                        binding.progress.setOnClickListener {
                            onItemListener.onCancel(messageItem.transcriptId, messageItem.messageId)
                        }
                        binding.chatImage.setOnClickListener { }
                    }
                    MediaStatus.DONE.name -> {
                        binding.chatWarning.visibility = GONE
                        binding.progress.visibility = GONE
                        binding.play.visibility = VISIBLE
                        binding.progress.setBindId("${messageItem.transcriptId ?: ""}${messageItem.messageId}")
                        binding.progress.setOnClickListener {}
                        binding.progress.setOnLongClickListener { false }
                        binding.chatImage.setOnClickListener {
                            onItemListener.onImageClick(messageItem, binding.chatImage)
                        }
                    }
                    MediaStatus.CANCELED.name -> {
                        binding.chatWarning.visibility = GONE
                        binding.progress.visibility = VISIBLE
                        binding.play.visibility = GONE
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
        }

        binding.chatTime.load(
            isMe,
            messageItem.createdAt,
            MessageStatus.DELIVERED.name,
            false,
            isRepresentative = false,
            isSecret = messageItem.isSecret(),
            isWhite = true,
        )

        dataWidth = messageItem.mediaWidth
        dataHeight = messageItem.mediaHeight
        dataUrl =
            if (messageItem.isLive()) {
                messageItem.thumbUrl
            } else {
                messageItem.absolutePath()
            }
        type = messageItem.type
        dataThumbImage = messageItem.thumbImage
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
            binding.chatLayout.setOnLongClickListener {
                onItemListener.onMenu(binding.chatJump, messageItem)
                true
            }
            chatJumpLayout(binding.chatJump, isMe, messageItem.messageId, R.id.chat_layout, onItemListener)
        }
    }

    private var dataUrl: String? = null
    private var type: String? = null
    private var dataThumbImage: String? = null
    private var dataWidth: Int? = null
    private var dataHeight: Int? = null

    override fun chatLayout(
        isMe: Boolean,
        isLast: Boolean,
        isBlink: Boolean,
    ) {
        super.chatLayout(isMe, isLast, isBlink)
        if (isMe) {
            (binding.chatLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 1f
            (binding.chatImageLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 1f
            (binding.durationTv.layoutParams as ViewGroup.MarginLayoutParams).marginStart = dp4
            (binding.liveTv.layoutParams as ViewGroup.MarginLayoutParams).marginStart = dp4
            (binding.chatTime.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = dp10
        } else {
            (binding.chatLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 0f
            (binding.chatImageLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 0f
            (binding.durationTv.layoutParams as ViewGroup.MarginLayoutParams).marginStart = dp10
            (binding.liveTv.layoutParams as ViewGroup.MarginLayoutParams).marginStart = dp10
            (binding.chatTime.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = dp3
        }

        var width = mediaWidth - dp6
        when {
            isLast -> {
                width = mediaWidth
                (binding.chatImage.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 0
                (binding.chatImage.layoutParams as ViewGroup.MarginLayoutParams).marginStart = 0
            }
            isMe -> {
                (binding.chatImage.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = dp6
                (binding.chatImage.layoutParams as ViewGroup.MarginLayoutParams).marginStart = 0
            }
            else -> {
                (binding.chatImage.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 0
                (binding.chatImage.layoutParams as ViewGroup.MarginLayoutParams).marginStart = dp6
            }
        }
        if (dataWidth == null || dataHeight == null ||
            dataWidth!! <= 0 || dataHeight!! <= 0
        ) {
            binding.chatImage.layoutParams.width = width
            binding.chatImage.layoutParams.height = width
        } else {
            binding.chatImage.layoutParams.width = width
            binding.chatImage.layoutParams.height = width * dataHeight!! / dataWidth!!
        }

        val mark =
            when {
                isMe && isLast -> R.drawable.chat_mark_image_me
                isMe -> R.drawable.chat_mark_image
                !isMe && isLast -> R.drawable.chat_mark_image_other
                else -> R.drawable.chat_mark_image
            }

        binding.chatImage.setShape(mark)
        if (type == MessageCategory.PLAIN_LIVE.name || type == MessageCategory.SIGNAL_LIVE.name || type == MessageCategory.ENCRYPTED_LIVE.name) {
            binding.chatImage.loadImageMark(dataUrl, R.drawable.image_holder, mark)
        } else {
            binding.chatImage.loadVideoMark(dataUrl, dataThumbImage, mark)
        }
    }
}
