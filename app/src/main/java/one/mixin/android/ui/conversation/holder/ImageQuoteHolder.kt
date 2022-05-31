package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import one.mixin.android.Constants.Colors.SELECT_COLOR
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.databinding.ItemChatImageQuoteBinding
import one.mixin.android.event.ExpiredEvent
import one.mixin.android.extension.dp
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.loadLongImageMark
import one.mixin.android.extension.round
import one.mixin.android.job.MixinJobManager.Companion.getAttachmentProcess
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.ui.conversation.holder.base.MediaHolder
import one.mixin.android.ui.conversation.holder.base.Terminable
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.QuoteMessageItem
import one.mixin.android.vo.absolutePath
import one.mixin.android.vo.isSecret
import kotlin.math.min

class ImageQuoteHolder constructor(val binding: ItemChatImageQuoteBinding) : MediaHolder(binding.root), Terminable {

    private val dp16 = itemView.context.dpToPx(16f)

    init {
        val radius = itemView.context.dpToPx(4f).toFloat()
        binding.chatImageLayout.round(radius)
        binding.chatTime.round(radius)
    }

    override fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean) {
        super.chatLayout(isMe, isLast, isBlink)
        if (isMe) {
            (binding.chatMsgLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 1f
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
            (binding.chatMsgLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 0f

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

    private var onItemListener: ConversationAdapter.OnItemListener? = null

    fun bind(
        messageItem: MessageItem,
        isLast: Boolean,
        isFirst: Boolean = false,
        hasSelect: Boolean,
        isSelect: Boolean,
        isRepresentative: Boolean,
        onItemListener: ConversationAdapter.OnItemListener
    ) {
        super.bind(messageItem)
        this.onItemListener = onItemListener
        if (hasSelect && isSelect) {
            itemView.setBackgroundColor(SELECT_COLOR)
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT)
        }

        binding.chatQuoteLayout.setRatio(messageItem.mediaWidth!!.toFloat() / messageItem.mediaHeight!!.toFloat())
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
            binding.chatName.setTextColor(getColorById(messageItem.userId))
            binding.chatName.setOnClickListener { onItemListener.onUserClick(messageItem.userId) }
        } else {
            binding.chatName.visibility = View.GONE
        }

        if (messageItem.appId != null) {
            binding.chatName.setCompoundDrawables(null, null, botIcon, null)
            binding.chatName.compoundDrawablePadding = 3.dp
        } else {
            binding.chatName.setCompoundDrawables(null, null, null, null)
        }
        binding.chatTime.load(isMe, messageItem.createdAt, messageItem.status, messageItem.isPin ?: false, isRepresentative, messageItem.isSecret(), true)
        val quoteMessage = GsonHelper.customGson.fromJson(messageItem.quoteContent, QuoteMessageItem::class.java)
        binding.chatQuote.bind(quoteMessage)
        binding.chatQuote.setOnClickListener {
            if (!hasSelect) {
                onItemListener.onQuoteMessageClick(messageItem.messageId, messageItem.quoteId)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
            }
        }

        chatLayout(isMe, isLast)
        chatJumpLayout(binding.chatJump, isMe, messageItem.expireIn, R.id.chat_msg_layout)
    }
    override fun onRead(messageItem: MessageItem) {
        if (messageItem.expireIn != null) {
            RxBus.publish(ExpiredEvent(messageItem.messageId, messageItem.expireIn))
        }
    }
}
