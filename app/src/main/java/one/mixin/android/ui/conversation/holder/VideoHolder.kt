package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import one.mixin.android.Constants.Colors.SELECT_COLOR
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.databinding.ItemChatVideoBinding
import one.mixin.android.event.ExpiredEvent
import one.mixin.android.extension.dp
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.fileSize
import one.mixin.android.extension.formatMillis
import one.mixin.android.extension.loadImageMark
import one.mixin.android.extension.loadVideoMark
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.round
import one.mixin.android.job.MixinJobManager.Companion.getAttachmentProcess
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.ui.conversation.holder.base.MediaHolder
import one.mixin.android.ui.conversation.holder.base.Terminable
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.absolutePath
import one.mixin.android.vo.isLive
import one.mixin.android.vo.isSecret

class VideoHolder constructor(val binding: ItemChatVideoBinding) :
    MediaHolder(binding.root),
    Terminable {

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
        messageItem: MessageItem,
        isLast: Boolean,
        isFirst: Boolean,
        hasSelect: Boolean,
        isSelect: Boolean,
        isRepresentative: Boolean,
        onItemListener: ConversationAdapter.OnItemListener
    ) {
        super.bind(messageItem)
        if (hasSelect && isSelect) {
            itemView.setBackgroundColor(SELECT_COLOR)
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT)
        }
        itemView.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
            }
        }

        itemView.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(messageItem, absoluteAdapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
                true
            }
        }

        val isMe = meId == messageItem.userId
        if (isFirst && !isMe) {
            binding.chatName.visibility = VISIBLE
            binding.chatName.text = messageItem.userFullName
            if (messageItem.appId != null) {
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
            binding.progress.setBindId(messageItem.messageId)
            binding.progress.setOnClickListener {}
            binding.progress.setOnLongClickListener { false }
            binding.chatImage.setOnLongClickListener {
                if (!hasSelect) {
                    onItemListener.onLongClick(messageItem, absoluteAdapterPosition)
                } else {
                    true
                }
            }
            binding.chatImage.setOnClickListener {
                if (hasSelect) {
                    onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
                } else {
                    onItemListener.onImageClick(messageItem, binding.chatImage)
                }
            }
        } else {
            binding.liveTv.visibility = GONE
            when (messageItem.mediaStatus) {
                MediaStatus.DONE.name -> {
                    binding.durationTv.bindId(null)
                    messageItem.mediaDuration.notNullWithElse(
                        {
                            binding.durationTv.visibility = VISIBLE
                            binding.durationTv.text = it.toLongOrNull()?.formatMillis() ?: ""
                        },
                        {
                            binding.durationTv.visibility = GONE
                        }
                    )
                }
                MediaStatus.PENDING.name -> {
                    messageItem.mediaSize.notNullWithElse(
                        {
                            binding.durationTv.visibility = VISIBLE
                            if (it == 0L) {
                                binding.durationTv.bindId(messageItem.messageId)
                            } else {
                                binding.durationTv.text = it.fileSize()
                                binding.durationTv.bindId(null)
                            }
                        },
                        {
                            binding.durationTv.bindId(null)
                            binding.durationTv.visibility = GONE
                        }
                    )
                }
                else -> {
                    messageItem.mediaSize.notNullWithElse(
                        {
                            if (it == 0L) {
                                binding.durationTv.visibility = GONE
                            } else {
                                binding.durationTv.visibility = VISIBLE
                                binding.durationTv.text = it.fileSize()
                            }
                        },
                        {
                            binding.durationTv.visibility = GONE
                        }
                    )
                    binding.durationTv.bindId(null)
                }
            }
            messageItem.mediaStatus?.let {
                when (it) {
                    MediaStatus.EXPIRED.name -> {
                        binding.chatWarning.visibility = VISIBLE
                        binding.progress.visibility = GONE
                        binding.play.visibility = GONE
                        binding.chatImage.setOnLongClickListener {
                            if (!hasSelect) {
                                onItemListener.onLongClick(messageItem, absoluteAdapterPosition)
                            } else {
                                true
                            }
                        }
                        binding.chatImage.setOnClickListener {
                            if (hasSelect) {
                                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
                            }
                        }
                    }
                    MediaStatus.PENDING.name -> {
                        binding.chatWarning.visibility = GONE
                        binding.progress.visibility = VISIBLE
                        binding.play.visibility = GONE
                        binding.progress.enableLoading(getAttachmentProcess(messageItem.messageId))
                        binding.progress.setBindOnly(messageItem.messageId)
                        binding.progress.setOnLongClickListener {
                            if (!hasSelect) {
                                onItemListener.onLongClick(messageItem, absoluteAdapterPosition)
                            } else {
                                false
                            }
                        }
                        binding.progress.setOnClickListener {
                            if (hasSelect) {
                                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
                            } else {
                                onItemListener.onCancel(messageItem.messageId)
                            }
                        }
                        binding.chatImage.setOnClickListener { }
                        binding.chatImage.setOnLongClickListener { false }
                    }
                    MediaStatus.DONE.name -> {
                        binding.chatWarning.visibility = GONE
                        binding.progress.visibility = GONE
                        binding.play.visibility = VISIBLE
                        binding.progress.setBindId(messageItem.messageId)
                        binding.progress.setOnClickListener {}
                        binding.progress.setOnLongClickListener { false }
                        binding.chatImage.setOnLongClickListener {
                            if (!hasSelect) {
                                onItemListener.onLongClick(messageItem, absoluteAdapterPosition)
                            } else {
                                true
                            }
                        }
                        binding.chatImage.setOnClickListener {
                            if (hasSelect) {
                                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
                            } else {
                                onItemListener.onImageClick(messageItem, binding.chatImage)
                            }
                        }
                    }
                    MediaStatus.CANCELED.name -> {
                        binding.chatWarning.visibility = GONE
                        binding.progress.visibility = VISIBLE
                        binding.play.visibility = GONE
                        if (isMe) {
                            binding.progress.enableUpload()
                        } else {
                            binding.progress.enableDownload()
                        }
                        binding.progress.setBindId(messageItem.messageId)
                        binding.progress.setProgress(-1)
                        binding.progress.setOnLongClickListener {
                            if (!hasSelect) {
                                onItemListener.onLongClick(messageItem, absoluteAdapterPosition)
                            } else {
                                false
                            }
                        }
                        binding.progress.setOnClickListener {
                            if (hasSelect) {
                                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
                            } else {
                                if (isMe) {
                                    onItemListener.onRetryUpload(messageItem.messageId)
                                } else {
                                    onItemListener.onRetryDownload(messageItem.messageId)
                                }
                            }
                        }
                        binding.chatImage.setOnClickListener {}
                        binding.chatImage.setOnLongClickListener { false }
                    }
                }
            }
        }

        binding.chatTime.load(
            isMe,
            messageItem.createdAt,
            messageItem.status,
            messageItem.isPin ?: false,
            isRepresentative = isRepresentative,
            isSecret = messageItem.isSecret(),
            isWhite = true
        )

        dataWidth = messageItem.mediaWidth
        dataHeight = messageItem.mediaHeight
        dataUrl = if (messageItem.isLive()) {
            messageItem.thumbUrl
        } else {
            messageItem.absolutePath()
        }
        type = messageItem.type
        dataThumbImage = messageItem.thumbImage
        chatJumpLayout(binding.chatJump, isMe, messageItem.expireIn, R.id.chat_layout)
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

        val mark = when {
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

    override fun onRead(messageItem: MessageItem) {
        if (messageItem.expireIn != null) {
            RxBus.publish(ExpiredEvent(messageItem.messageId, messageItem.expireIn))
        }
    }
}
