package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import androidx.core.view.isVisible
import one.mixin.android.Constants
import one.mixin.android.Constants.Colors.LINK_COLOR
import one.mixin.android.Constants.Colors.SELECT_COLOR
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatImageCaptionBinding
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dp
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.renderMessage
import one.mixin.android.extension.roundTopOrBottom
import one.mixin.android.job.MixinJobManager.Companion.getAttachmentProcess
import one.mixin.android.ui.conversation.adapter.MessageAdapter
import one.mixin.android.ui.conversation.holder.base.MediaHolder
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.mention.MentionRenderCache
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.QuoteMessageItem
import one.mixin.android.vo.absolutePath
import one.mixin.android.vo.isSecret
import one.mixin.android.widget.linktext.AutoLinkMode
import kotlin.math.min

class ImageCaptionHolder(val binding: ItemChatImageCaptionBinding) :
    MediaHolder(binding.root) {
    private val radius = 6.dp.toFloat()

    init {
        binding.chatTv.addAutoLinkMode(AutoLinkMode.MODE_URL, AutoLinkMode.MODE_MENTION)
        binding.chatTv.setUrlModeColor(LINK_COLOR)
        binding.chatTv.setMentionModeColor(LINK_COLOR)
        binding.chatTv.setSelectedStateColor(SELECT_COLOR)
        binding.root.context.defaultSharedPreferences.getInt(Constants.Account.PREF_TEXT_SIZE, 14).apply {
            if (this != 14) {
                val textSize = this.toFloat()
                binding.chatTime.changeSize(textSize - 4f)
                binding.chatName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize)
                binding.chatTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize)
            }
        }
    }

    override fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean) {
        super.chatLayout(isMe, isLast, isBlink)
        if (isMe) {
            binding.chatMsgLayout.gravity = Gravity.END
            if (isLast) {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_reply_me_last,
                    R.drawable.chat_bubble_reply_me_last_night
                )
            } else {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_reply_me,
                    R.drawable.chat_bubble_reply_me_night
                )
            }
        } else {
            binding.chatMsgLayout.gravity = Gravity.START

            if (isLast) {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_reply_other_last,
                    R.drawable.chat_bubble_reply_other_last_night
                )
            } else {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_reply_other,
                    R.drawable.chat_bubble_reply_other_night
                )
            }
        }
    }

    private var onItemListener: MessageAdapter.OnItemListener? = null

    fun bind(
        messageItem: MessageItem,
        isLast: Boolean,
        isFirst: Boolean = false,
        hasSelect: Boolean,
        isSelect: Boolean,
        isRepresentative: Boolean,
        onItemListener: MessageAdapter.OnItemListener
    ) {
        super.bind(messageItem)
        this.onItemListener = onItemListener
        if (hasSelect && isSelect) {
            itemView.setBackgroundColor(SELECT_COLOR)
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT)
        }
        binding.chatImageLayout.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(messageItem, absoluteAdapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
                true
            }
        }

        binding.chatLayout.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(messageItem, absoluteAdapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
                true
            }
        }

        binding.chatImageLayout.setOnClickListener {
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

        itemView.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
            }
        }

        binding.chatTv.setAutoLinkOnClickListener { autoLinkMode, matchedText ->
            when (autoLinkMode) {
                AutoLinkMode.MODE_URL -> {
                    onItemListener.onUrlClick(matchedText)
                }

                AutoLinkMode.MODE_MENTION -> {
                    onItemListener.onMentionClick(matchedText)
                }

                else -> {
                }
            }
        }

        binding.chatTv.setAutoLinkOnLongClickListener { autoLinkMode, matchedText ->
            when (autoLinkMode) {
                AutoLinkMode.MODE_URL -> {
                    onItemListener.onUrlLongClick(matchedText)
                }

                else -> {
                }
            }
        }

        if (messageItem.mentions?.isNotBlank() == true) {
            val mentionRenderContext = MentionRenderCache.singleton.getMentionRenderContext(
                messageItem.mentions
            )
            binding.chatTv.renderMessage(messageItem.caption, null, mentionRenderContext)
        } else {
            binding.chatTv.text = messageItem.caption
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

        val dataWidth = messageItem.mediaWidth
        val dataHeight = messageItem.mediaHeight
        val width = mediaWidth - dp6
        if (dataWidth!! <= 0 || dataHeight!! <= 0) {
            binding.chatImageLayout.layoutParams.width = width
            binding.chatImageLayout.layoutParams.height = width
        } else {
            binding.chatImageLayout.layoutParams.width = width
            binding.chatImageLayout.layoutParams.height = min(width * dataHeight / dataWidth, mediaHeight)
        }
        binding.chatImage.loadImage(messageItem.absolutePath(), null, base64Holder = messageItem.thumbImage)

        val isMe = meId == messageItem.userId
        binding.chatTime.load(
            isMe,
            messageItem.createdAt,
            messageItem.status,
            isPin = messageItem.isPin ?: false,
            isRepresentative = isRepresentative,
            isSecret = messageItem.isSecret(),
        )
        if (isFirst && !isMe) {
            binding.chatName.visibility = View.VISIBLE
            binding.chatName.setMessageName(messageItem)
            binding.chatName.setTextColor(getColorById(messageItem.userId))
            binding.chatName.setOnClickListener { onItemListener.onUserClick(messageItem.userId) }
        } else {
            binding.chatName.visibility = View.GONE
        }

        if (messageItem.quoteContent.isNullOrEmpty()) {
            binding.chatQuote.isVisible = false
            binding.chatImageLayout.roundTopOrBottom(radius, top = true, bottom = false)
        } else {
            binding.chatQuote.isVisible = true
            binding.chatImageLayout.clipToOutline = false
            val quoteMessage = GsonHelper.customGson.fromJson(
                messageItem.quoteContent,
                QuoteMessageItem::class.java
            )
            binding.chatQuote.bind(quoteMessage)
            binding.chatQuote.setOnClickListener {
                if (!hasSelect) {
                    onItemListener.onQuoteMessageClick(messageItem.messageId, messageItem.quoteId)
                } else {
                    onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
                }
            }
        }
        chatLayout(isMe, isLast)
    }
}