package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import one.mixin.android.Constants.Colors.SELECT_COLOR
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.databinding.ItemChatImageBinding
import one.mixin.android.event.ExpiredEvent
import one.mixin.android.extension.dp
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.loadGifMark
import one.mixin.android.extension.loadImageMark
import one.mixin.android.extension.loadLongImageMark
import one.mixin.android.extension.round
import one.mixin.android.job.MixinJobManager.Companion.getAttachmentProcess
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.ui.conversation.holder.base.MediaHolder
import one.mixin.android.ui.conversation.holder.base.Terminable
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.absolutePath
import one.mixin.android.vo.isSecret
import one.mixin.android.widget.gallery.MimeType
import kotlin.math.min

class ImageHolder constructor(val binding: ItemChatImageBinding) : MediaHolder(binding.root), Terminable {

    init {
        val radius = itemView.context.dpToPx(4f).toFloat()
        binding.chatImage.round(radius)
        binding.chatTime.round(radius)
        binding.progress.round(radius)
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
            binding.chatName.visibility = View.VISIBLE
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
            binding.chatName.visibility = View.GONE
        }

        messageItem.mediaStatus?.let {
            when (it) {
                MediaStatus.EXPIRED.name -> {
                    binding.chatWarning.visibility = View.VISIBLE
                    binding.progress.visibility = View.GONE
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
                    binding.chatWarning.visibility = View.GONE
                    binding.progress.visibility = View.VISIBLE
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
                    binding.chatWarning.visibility = View.GONE
                    binding.progress.visibility = View.GONE
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
                    binding.chatWarning.visibility = View.GONE
                    binding.progress.visibility = View.VISIBLE
                    if (isMe && messageItem.mediaUrl != null) {
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
                            if (isMe && messageItem.mediaUrl != null) {
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
        dataUrl = messageItem.absolutePath()
        dataThumbImage = messageItem.thumbImage
        dataSize = messageItem.mediaSize
        isGif = messageItem.mediaMimeType.equals(MimeType.GIF.toString(), true)
        chatJumpLayout(binding.chatJump, isMe, messageItem.expireIn, R.id.chat_layout)
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
            (binding.chatLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 1f
            (binding.chatImageLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 1f
            (binding.chatTime.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = dp10
        } else {
            (binding.chatLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 0f
            (binding.chatImageLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 0f
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
            binding.chatImage.layoutParams.height =
                min(width * dataHeight!! / dataWidth!!, mediaHeight)
        }
        val mark = when {
            isMe && isLast -> R.drawable.chat_mark_image_me
            isMe -> R.drawable.chat_mark_image
            !isMe && isLast -> R.drawable.chat_mark_image_other
            else -> R.drawable.chat_mark_image
        }

        binding.chatImage.setShape(mark)
        binding.largeImageIv.isVisible = binding.chatImage.layoutParams.height == mediaHeight
        if (isBlink) {
            when {
                isGif -> handleGif(mark)
                binding.chatImage.layoutParams.height == mediaHeight -> binding.chatImage.loadLongImageMark(dataUrl, mark)
                else -> binding.chatImage.loadImageMark(dataUrl, mark)
            }
        } else {
            when {
                isGif -> handleGif(mark)
                binding.chatImage.layoutParams.height == mediaHeight -> binding.chatImage.loadLongImageMark(dataUrl, dataThumbImage, mark)
                else -> binding.chatImage.loadImageMark(dataUrl, dataThumbImage, mark)
            }
        }
    }

    private fun handleGif(mark: Int) {
        if (dataSize == null || dataSize == 0L) { // un-downloaded giphy
            binding.chatImage.loadGifMark(dataThumbImage, null, mark, false)
        } else {
            binding.chatImage.loadGifMark(dataUrl, dataThumbImage, mark, true)
        }
    }

    override fun onRead(messageItem: MessageItem) {
        if (messageItem.expireIn != null) {
            RxBus.publish(ExpiredEvent(messageItem.messageId, messageItem.expireIn))
        }
    }
}
