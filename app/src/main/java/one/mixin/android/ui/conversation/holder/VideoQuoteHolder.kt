package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import one.mixin.android.Constants.Colors.SELECT_COLOR
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatVideoQuoteBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.fileSize
import one.mixin.android.extension.formatMillis
import one.mixin.android.extension.loadVideo
import one.mixin.android.extension.realSize
import one.mixin.android.extension.round
import one.mixin.android.job.MixinJobManager.Companion.getAttachmentProcess
import one.mixin.android.ui.conversation.adapter.MessageAdapter
import one.mixin.android.ui.conversation.holder.base.BaseViewHolder
import one.mixin.android.ui.conversation.holder.base.Terminable
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.absolutePath
import one.mixin.android.vo.isSecret

class VideoQuoteHolder constructor(val binding: ItemChatVideoQuoteBinding) :
    BaseViewHolder(binding.root),
    Terminable {
        private val dp16 = itemView.context.dpToPx(16f)
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

        private var onItemListener: MessageAdapter.OnItemListener? = null

        fun bind(
            messageItem: MessageItem,
            isLast: Boolean,
            isFirst: Boolean = false,
            hasSelect: Boolean,
            isSelect: Boolean,
            isRepresentative: Boolean,
            onItemListener: MessageAdapter.OnItemListener,
        ) {
            super.bind(messageItem)
            this.onItemListener = onItemListener
            if (hasSelect && isSelect) {
                itemView.setBackgroundColor(SELECT_COLOR)
            } else {
                itemView.setBackgroundColor(Color.TRANSPARENT)
            }

            binding.chatQuoteLayout.setRatio(messageItem.mediaWidth!!.toFloat() / messageItem.mediaHeight!!.toFloat())
            binding.chatLayout.setOnLongClickListener {
                if (!hasSelect) {
                    onItemListener.onLongClick(messageItem, absoluteAdapterPosition)
                } else {
                    onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
                    true
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
                        binding.play.visibility = View.GONE
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
                        binding.play.visibility = View.VISIBLE
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
                        binding.play.visibility = View.GONE
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

            binding.chatImage.loadVideo(
                messageItem.absolutePath(),
                messageItem.thumbImage,
                minWidth,
                minWidth * messageItem.mediaHeight / messageItem.mediaWidth,
            )

            val isMe = meId == messageItem.userId
            if (isFirst && !isMe) {
                binding.chatName.visibility = View.VISIBLE
                binding.chatName.setName(messageItem)
                binding.chatName.setTextColor(getColorById(messageItem.userId))
                binding.chatName.setOnClickListener { onItemListener.onUserClick(messageItem.userId) }
            } else {
                binding.chatName.visibility = View.GONE
            }

            binding.chatTime.load(
                isMe,
                messageItem.createdAt,
                messageItem.status,
                messageItem.isPin ?: false,
                isRepresentative = isRepresentative,
                isSecret = messageItem.isSecret(),
                isWhite = true,
            )

            binding.chatQuote.bind(fromJsonQuoteMessage(messageItem.quoteContent))
            binding.chatQuote.setOnClickListener {
                if (!hasSelect) {
                    onItemListener.onQuoteMessageClick(messageItem.messageId, messageItem.quoteId)
                } else {
                    onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
                }
            }
            chatJumpLayout(binding.chatJump, isMe, messageItem.expireIn, messageItem.expireAt, R.id.chat_msg_layout)
            chatLayout(isMe, isLast)
        }
    }
