package one.mixin.android.ui.conversation.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.collection.arraySetOf
import androidx.recyclerview.widget.RecyclerView
import io.noties.markwon.Markwon
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatActionBinding
import one.mixin.android.databinding.ItemChatActionCardBinding
import one.mixin.android.databinding.ItemChatAudioBinding
import one.mixin.android.databinding.ItemChatAudioQuoteBinding
import one.mixin.android.databinding.ItemChatCallBinding
import one.mixin.android.databinding.ItemChatCardBinding
import one.mixin.android.databinding.ItemChatContactCardBinding
import one.mixin.android.databinding.ItemChatContactCardQuoteBinding
import one.mixin.android.databinding.ItemChatFileBinding
import one.mixin.android.databinding.ItemChatFileQuoteBinding
import one.mixin.android.databinding.ItemChatHyperlinkBinding
import one.mixin.android.databinding.ItemChatImageBinding
import one.mixin.android.databinding.ItemChatImageQuoteBinding
import one.mixin.android.databinding.ItemChatLocationBinding
import one.mixin.android.databinding.ItemChatPostBinding
import one.mixin.android.databinding.ItemChatRecallBinding
import one.mixin.android.databinding.ItemChatSecretBinding
import one.mixin.android.databinding.ItemChatSnapshotBinding
import one.mixin.android.databinding.ItemChatStickerBinding
import one.mixin.android.databinding.ItemChatStrangerBinding
import one.mixin.android.databinding.ItemChatSystemBinding
import one.mixin.android.databinding.ItemChatTextBinding
import one.mixin.android.databinding.ItemChatTextQuoteBinding
import one.mixin.android.databinding.ItemChatTimeBinding
import one.mixin.android.databinding.ItemChatTranscriptBinding
import one.mixin.android.databinding.ItemChatTransparentBinding
import one.mixin.android.databinding.ItemChatUnknownBinding
import one.mixin.android.databinding.ItemChatUnreadBinding
import one.mixin.android.databinding.ItemChatVideoBinding
import one.mixin.android.databinding.ItemChatVideoQuoteBinding
import one.mixin.android.databinding.ItemChatWaitingBinding
import one.mixin.android.db.fetcher.MessageFetcher.Companion.SCROLL_THRESHOLD
import one.mixin.android.extension.hashForDate
import one.mixin.android.extension.isSameDay
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.session.Session
import one.mixin.android.ui.conversation.chat.CompressedList
import one.mixin.android.ui.conversation.holder.ActionCardHolder
import one.mixin.android.ui.conversation.holder.ActionHolder
import one.mixin.android.ui.conversation.holder.AudioHolder
import one.mixin.android.ui.conversation.holder.AudioQuoteHolder
import one.mixin.android.ui.conversation.holder.CallHolder
import one.mixin.android.ui.conversation.holder.CardHolder
import one.mixin.android.ui.conversation.holder.ContactCardHolder
import one.mixin.android.ui.conversation.holder.ContactCardQuoteHolder
import one.mixin.android.ui.conversation.holder.FileHolder
import one.mixin.android.ui.conversation.holder.FileQuoteHolder
import one.mixin.android.ui.conversation.holder.GroupCallHolder
import one.mixin.android.ui.conversation.holder.HyperlinkHolder
import one.mixin.android.ui.conversation.holder.ImageHolder
import one.mixin.android.ui.conversation.holder.ImageQuoteHolder
import one.mixin.android.ui.conversation.holder.LocationHolder
import one.mixin.android.ui.conversation.holder.PinMessageHolder
import one.mixin.android.ui.conversation.holder.PostHolder
import one.mixin.android.ui.conversation.holder.RecallHolder
import one.mixin.android.ui.conversation.holder.SecretHolder
import one.mixin.android.ui.conversation.holder.SnapshotHolder
import one.mixin.android.ui.conversation.holder.StickerHolder
import one.mixin.android.ui.conversation.holder.StrangerHolder
import one.mixin.android.ui.conversation.holder.SystemHolder
import one.mixin.android.ui.conversation.holder.TextHolder
import one.mixin.android.ui.conversation.holder.TextQuoteHolder
import one.mixin.android.ui.conversation.holder.TimeHolder
import one.mixin.android.ui.conversation.holder.TranscriptHolder
import one.mixin.android.ui.conversation.holder.TransparentHolder
import one.mixin.android.ui.conversation.holder.UnknownHolder
import one.mixin.android.ui.conversation.holder.VideoHolder
import one.mixin.android.ui.conversation.holder.VideoQuoteHolder
import one.mixin.android.ui.conversation.holder.WaitingHolder
import one.mixin.android.ui.conversation.holder.base.BaseMentionHolder
import one.mixin.android.ui.conversation.holder.base.BaseViewHolder
import one.mixin.android.ui.conversation.holder.base.Terminable
import one.mixin.android.vo.AppCardData
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.User
import one.mixin.android.vo.create
import one.mixin.android.vo.isAudio
import one.mixin.android.vo.isCallMessage
import one.mixin.android.vo.isContact
import one.mixin.android.vo.isData
import one.mixin.android.vo.isGroupCall
import one.mixin.android.vo.isImage
import one.mixin.android.vo.isLive
import one.mixin.android.vo.isLocation
import one.mixin.android.vo.isPin
import one.mixin.android.vo.isPost
import one.mixin.android.vo.isRecall
import one.mixin.android.vo.isSticker
import one.mixin.android.vo.isText
import one.mixin.android.vo.isTranscript
import one.mixin.android.vo.isVideo
import one.mixin.android.widget.MixinStickyRecyclerHeadersAdapter
import kotlin.math.abs

class MessageAdapter(
    val data: CompressedList<MessageItem>,
    private val miniMarkwon: Markwon,
    val onItemListener: MessageAdapter.OnItemListener,
    val previousPage: (String) -> Unit,
    val nextPage: (String) -> Unit,
    val isGroup: Boolean,
    var unreadMessageId: String?,
    var recipient: User? = null,
    private val isBot: Boolean = false,
    private val isSecret: Boolean = true,
    var keyword: String? = null,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), MixinStickyRecyclerHeadersAdapter<TimeHolder> {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder =
        when (viewType) {
            MessageAdapter.TEXT_TYPE -> {
                TextHolder(ItemChatTextBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            MessageAdapter.TEXT_QUOTE_TYPE -> {
                TextQuoteHolder(ItemChatTextQuoteBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            MessageAdapter.POST_TYPE -> {
                PostHolder(ItemChatPostBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            MessageAdapter.IMAGE_TYPE -> {
                ImageHolder(ItemChatImageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            MessageAdapter.IMAGE_QUOTE_TYPE -> {
                ImageQuoteHolder(ItemChatImageQuoteBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            MessageAdapter.SYSTEM_TYPE -> {
                SystemHolder(ItemChatSystemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            MessageAdapter.CARD_TYPE -> {
                CardHolder(ItemChatCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            MessageAdapter.SNAPSHOT_TYPE -> {
                SnapshotHolder(ItemChatSnapshotBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            MessageAdapter.WAITING_TYPE -> {
                WaitingHolder(ItemChatWaitingBinding.inflate(LayoutInflater.from(parent.context), parent, false), onItemListener)
            }
            MessageAdapter.STRANGER_TYPE -> {
                StrangerHolder(ItemChatStrangerBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            MessageAdapter.UNKNOWN_TYPE -> {
                UnknownHolder(ItemChatUnknownBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            MessageAdapter.FILE_TYPE -> {
                FileHolder(ItemChatFileBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            MessageAdapter.FILE_QUOTE_TYPE -> {
                FileQuoteHolder(ItemChatFileQuoteBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            MessageAdapter.AUDIO_TYPE -> {
                AudioHolder(ItemChatAudioBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            MessageAdapter.AUDIO_QUOTE_TYPE -> {
                AudioQuoteHolder(ItemChatAudioQuoteBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            MessageAdapter.STICKER_TYPE -> {
                StickerHolder(ItemChatStickerBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            MessageAdapter.ACTION_TYPE -> {
                ActionHolder(ItemChatActionBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            MessageAdapter.ACTION_CARD_TYPE -> {
                ActionCardHolder(ItemChatActionCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            MessageAdapter.LINK_TYPE -> {
                HyperlinkHolder(ItemChatHyperlinkBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            MessageAdapter.CONTACT_CARD_TYPE -> {
                ContactCardHolder(ItemChatContactCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            MessageAdapter.CONTACT_CARD_QUOTE_TYPE -> {
                ContactCardQuoteHolder(ItemChatContactCardQuoteBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            MessageAdapter.VIDEO_TYPE -> {
                VideoHolder(ItemChatVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            MessageAdapter.VIDEO_QUOTE_TYPE -> {
                VideoQuoteHolder(ItemChatVideoQuoteBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            MessageAdapter.SECRET_TYPE -> {
                SecretHolder(ItemChatSecretBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            MessageAdapter.CALL_TYPE -> {
                CallHolder(ItemChatCallBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            MessageAdapter.RECALL_TYPE -> {
                RecallHolder(ItemChatRecallBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            MessageAdapter.LOCATION_TYPE -> {
                LocationHolder(ItemChatLocationBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            MessageAdapter.GROUP_CALL_TYPE -> {
                GroupCallHolder(ItemChatSystemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            MessageAdapter.TRANSCRIPT_TYPE -> {
                TranscriptHolder(ItemChatTranscriptBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            MessageAdapter.PIN_TYPE -> {
                PinMessageHolder(ItemChatSystemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            else -> {
                TransparentHolder(ItemChatTransparentBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        getItem(position)?.let {
            when (getItemViewType(position)) {
                MessageAdapter.TEXT_TYPE -> {
                    (holder as TextHolder).bind(
                        it,
                        keyword,
                        isLast(position),
                        isFirst(position),
                        selectSet.size > 0,
                        isSelect(position),
                        isRepresentative(it),
                        onItemListener,
                    )
                }
                MessageAdapter.TEXT_QUOTE_TYPE -> {
                    (holder as TextQuoteHolder).bind(
                        it,
                        keyword,
                        isLast(position),
                        isFirst(position),
                        selectSet.size > 0,
                        isSelect(position),
                        isRepresentative(it),
                        onItemListener,
                    )
                }
                MessageAdapter.POST_TYPE -> {
                    (holder as PostHolder).bind(
                        it,
                        isLast(position),
                        isFirst(position),
                        selectSet.size > 0,
                        isSelect(position),
                        isRepresentative(it),
                        onItemListener,
                        miniMarkwon,
                    )
                }
                MessageAdapter.IMAGE_TYPE -> {
                    (holder as ImageHolder).bind(
                        it,
                        isLast(position),
                        isFirst(position),
                        selectSet.size > 0,
                        isSelect(position),
                        isRepresentative(it),
                        onItemListener,
                    )
                }
                MessageAdapter.IMAGE_QUOTE_TYPE -> {
                    (holder as ImageQuoteHolder).bind(
                        it,
                        isLast(position),
                        isFirst(position),
                        selectSet.size > 0,
                        isSelect(position),
                        isRepresentative(it),
                        onItemListener,
                    )
                }
                MessageAdapter.VIDEO_TYPE -> {
                    (holder as VideoHolder).bind(
                        it,
                        isLast(position),
                        isFirst(position),
                        selectSet.size > 0,
                        isSelect(position),
                        isRepresentative(it),
                        onItemListener,
                    )
                }
                MessageAdapter.VIDEO_QUOTE_TYPE -> {
                    (holder as VideoQuoteHolder).bind(
                        it,
                        isLast(position),
                        isFirst(position),
                        selectSet.size > 0,
                        isSelect(position),
                        isRepresentative(it),
                        onItemListener,
                    )
                }
                MessageAdapter.AUDIO_TYPE -> {
                    (holder as AudioHolder).bind(
                        it,
                        isFirst(position),
                        isLast(position),
                        selectSet.size > 0,
                        isSelect(position),
                        isRepresentative(it),
                        onItemListener,
                    )
                }
                MessageAdapter.AUDIO_QUOTE_TYPE -> {
                    (holder as AudioQuoteHolder).bind(
                        it,
                        isFirst(position),
                        isLast(position),
                        selectSet.size > 0,
                        isSelect(position),
                        isRepresentative(it),
                        onItemListener,
                    )
                }
                MessageAdapter.SYSTEM_TYPE -> {
                    (holder as SystemHolder).bind(
                        it,
                        selectSet.size > 0,
                        isSelect(position),
                        onItemListener,
                    )
                }
                MessageAdapter.CARD_TYPE -> {
                    (holder as CardHolder).bind(it)
                }
                MessageAdapter.SNAPSHOT_TYPE -> {
                    (holder as SnapshotHolder).bind(
                        it,
                        isLast(position),
                        selectSet.size > 0,
                        isSelect(position),
                        onItemListener,
                    )
                }
                MessageAdapter.FILE_TYPE -> {
                    (holder as FileHolder).bind(
                        it,
                        keyword,
                        isFirst(position),
                        isLast(position),
                        selectSet.size > 0,
                        isSelect(position),
                        isRepresentative(it),
                        onItemListener,
                    )
                }
                MessageAdapter.FILE_QUOTE_TYPE -> {
                    (holder as FileQuoteHolder).bind(
                        it,
                        keyword,
                        isFirst(position),
                        isLast(position),
                        selectSet.size > 0,
                        isSelect(position),
                        isRepresentative(it),
                        onItemListener,
                    )
                }
                MessageAdapter.WAITING_TYPE -> {
                    (holder as WaitingHolder).bind(
                        it,
                        isLast(position),
                        isFirst(position),
                        onItemListener,
                    )
                }
                MessageAdapter.STRANGER_TYPE -> {
                    (holder as StrangerHolder).bind(onItemListener, isBot)
                }
                MessageAdapter.UNKNOWN_TYPE -> {
                    (holder as UnknownHolder).bind(
                        it,
                        isLast(position),
                        isFirst(position),
                        selectSet.size > 0,
                        isSelect(position),
                        onItemListener,
                    )
                }
                MessageAdapter.STICKER_TYPE -> {
                    (holder as StickerHolder).bind(
                        it,
                        isFirst(position),
                        selectSet.size > 0,
                        isSelect(position),
                        isRepresentative(it),
                        onItemListener,
                    )
                }
                MessageAdapter.LINK_TYPE -> {
                    (holder as HyperlinkHolder).bind(
                        it,
                        keyword,
                        isLast(position),
                        isFirst(position),
                        selectSet.size > 0,
                        isSelect(position),
                        isRepresentative(it),
                        onItemListener,
                    )
                }
                MessageAdapter.ACTION_TYPE -> {
                    (holder as ActionHolder).bind(
                        it,
                        isFirst(position),
                        selectSet.size > 0,
                        isSelect(position),
                        onItemListener,
                    )
                }
                MessageAdapter.ACTION_CARD_TYPE -> {
                    (holder as ActionCardHolder).bind(
                        it,
                        isFirst(position),
                        isLast(position),
                        selectSet.size > 0,
                        isSelect(position),
                        isRepresentative(it),
                        onItemListener,
                    )
                }
                MessageAdapter.CONTACT_CARD_TYPE -> {
                    (holder as ContactCardHolder).bind(
                        it,
                        isFirst(position),
                        isLast(position),
                        selectSet.size > 0,
                        isSelect(position),
                        isRepresentative(it),
                        onItemListener,
                    )
                }
                MessageAdapter.CONTACT_CARD_QUOTE_TYPE -> {
                    (holder as ContactCardQuoteHolder).bind(
                        it,
                        isFirst(position),
                        isLast(position),
                        selectSet.size > 0,
                        isSelect(position),
                        isRepresentative(it),
                        onItemListener,
                    )
                }
                MessageAdapter.SECRET_TYPE -> {
                    (holder as SecretHolder).bind(onItemListener)
                }
                MessageAdapter.CALL_TYPE -> {
                    (holder as CallHolder).bind(
                        it,
                        isFirst(position),
                        selectSet.size > 0,
                        isSelect(position),
                        onItemListener,
                    )
                }
                MessageAdapter.RECALL_TYPE -> {
                    (holder as RecallHolder).bind(
                        it,
                        isFirst(position),
                        isLast(position),
                        selectSet.size > 0,
                        isSelect(position),
                        onItemListener,
                    )
                }
                MessageAdapter.LOCATION_TYPE -> {
                    (holder as LocationHolder).bind(
                        it,
                        isLast(position),
                        isFirst(position),
                        selectSet.size > 0,
                        isSelect(position),
                        isRepresentative(it),
                        onItemListener,
                    )
                }
                MessageAdapter.GROUP_CALL_TYPE -> {
                    (holder as GroupCallHolder).bind(
                        it,
                        selectSet.size > 0,
                        isSelect(position),
                        onItemListener,
                    )
                }
                MessageAdapter.TRANSCRIPT_TYPE -> {
                    (holder as TranscriptHolder).bind(
                        it,
                        isLast(position),
                        isFirst(position),
                        selectSet.size > 0,
                        isSelect(position),
                        isRepresentative(it),
                        onItemListener,
                    )
                }
                MessageAdapter.PIN_TYPE -> {
                    (holder as PinMessageHolder).bind(
                        it,
                        selectSet.size > 0,
                        isSelect(position),
                        onItemListener,
                    )
                }
                else -> {
                }
            }
        }
    }

    private fun getItemType(messageItem: MessageItem?): Int =
        messageItem.notNullWithElse(
            { item ->
                when {
                    item.status == MessageStatus.UNKNOWN.name -> MessageAdapter.UNKNOWN_TYPE
                    item.type == MessageCategory.STRANGER.name -> MessageAdapter.STRANGER_TYPE
                    item.type == MessageCategory.SECRET.name -> MessageAdapter.SECRET_TYPE
                    item.status == MessageStatus.FAILED.name -> MessageAdapter.WAITING_TYPE
                    item.isText() -> {
                        if (!item.quoteId.isNullOrEmpty()) {
                            MessageAdapter.TEXT_QUOTE_TYPE
                        } else if (!item.siteName.isNullOrBlank() || !item.siteDescription.isNullOrBlank()) {
                            MessageAdapter.LINK_TYPE
                        } else {
                            MessageAdapter.TEXT_TYPE
                        }
                    }
                    item.isImage() -> {
                        if (!item.quoteId.isNullOrEmpty()) {
                            MessageAdapter.IMAGE_QUOTE_TYPE
                        } else {
                            MessageAdapter.IMAGE_TYPE
                        }
                    }
                    item.isSticker() -> MessageAdapter.STICKER_TYPE
                    item.isData() -> {
                        if (!item.quoteId.isNullOrEmpty()) {
                            MessageAdapter.FILE_QUOTE_TYPE
                        } else {
                            MessageAdapter.FILE_TYPE
                        }
                    }
                    item.type == MessageCategory.SYSTEM_CONVERSATION.name -> MessageAdapter.SYSTEM_TYPE
                    item.type == MessageCategory.SYSTEM_ACCOUNT_SNAPSHOT.name -> MessageAdapter.SNAPSHOT_TYPE
                    item.type == MessageCategory.APP_BUTTON_GROUP.name -> MessageAdapter.ACTION_TYPE
                    item.type == MessageCategory.APP_CARD.name -> MessageAdapter.ACTION_CARD_TYPE
                    item.isContact() -> {
                        if (!item.quoteId.isNullOrEmpty()) {
                            MessageAdapter.CONTACT_CARD_QUOTE_TYPE
                        } else {
                            MessageAdapter.CONTACT_CARD_TYPE
                        }
                    }
                    item.isVideo() or item.isLive() -> {
                        if (!item.quoteId.isNullOrEmpty()) {
                            MessageAdapter.VIDEO_QUOTE_TYPE
                        } else {
                            MessageAdapter.VIDEO_TYPE
                        }
                    }
                    item.isAudio() -> {
                        if (!item.quoteId.isNullOrEmpty()) {
                            MessageAdapter.AUDIO_QUOTE_TYPE
                        } else {
                            MessageAdapter.AUDIO_TYPE
                        }
                    }
                    item.isPost() -> MessageAdapter.POST_TYPE
                    item.isCallMessage() -> MessageAdapter.CALL_TYPE
                    item.isRecall() -> MessageAdapter.RECALL_TYPE
                    item.isLocation() -> MessageAdapter.LOCATION_TYPE
                    item.isGroupCall() -> MessageAdapter.GROUP_CALL_TYPE
                    item.isTranscript() -> MessageAdapter.TRANSCRIPT_TYPE
                    item.isPin() -> MessageAdapter.PIN_TYPE
                    else -> MessageAdapter.UNKNOWN_TYPE
                }
            },
            MessageAdapter.NULL_TYPE,
        )

    fun layoutPosition(position: Int): Int {
        if (isSecret) {
            return position + 1
        } else {
            return position
        }
    }

    // Item and position
    fun getItem(position: Int): MessageItem? {
        return if (position > itemCount - 1 || position < 0) {
            null
        } else if (isSecret && hasBottomView) {
            when (position) {
                itemCount - 1 -> create(
                    MessageCategory.STRANGER.name,
                    if (data.size > 0) {
                        data.last()?.createdAt
                    } else {
                        null
                    },
                )

                0 -> create(
                    MessageCategory.SECRET.name,
                    if (data.size > 0) {
                        data.first()?.createdAt
                    } else {
                        null
                    },
                )

                else -> getItemInternal(position - 1)
            }
        } else if (isSecret) {
            if (position == 0) {
                create(
                    MessageCategory.SECRET.name,
                    if (data.size > 0) {
                        data.first()?.createdAt
                    } else {
                        null
                    },
                )
            } else {
                getItemInternal(position - 1)
            }
        } else if (hasBottomView) {
            if (position == itemCount - 1) {
                create(
                    MessageCategory.STRANGER.name,
                    if (data.size > 0) {
                        data.last()?.createdAt
                    } else {
                        null
                    },
                )
            } else {
                getItemInternal(position)
            }
        } else {
            getItemInternal(position)
        }
    }

    private fun getItemInternal(position: Int): MessageItem? {
        if (position < SCROLL_THRESHOLD) {
            data.first()?.messageId?.let { id ->
                previousPage(id)
            }
        } else if (position > data.size - SCROLL_THRESHOLD - 1) {
            data.last()?.messageId?.let { id ->
                nextPage(id)
            }
        }
        return try {
            data[position]
        } catch (e: Exception) {
            null
        }
    }

    override fun isListLast(position: Int): Boolean {
        return position == itemCount - 1
    }

    fun isFirst(position: Int): Boolean {
        return if (isGroup || recipient != null) {
            val currentItem = getItem(position)
            if (!isGroup && (recipient?.isBot() == false || recipient?.userId == currentItem?.userId)) {
                return false
            }
            val previousItem = getItem(position - 1)
            when {
                currentItem == null ->
                    false
                previousItem == null ->
                    true
                previousItem.type == MessageCategory.MESSAGE_PIN.name ->
                    true
                previousItem.type == MessageCategory.SYSTEM_CONVERSATION.name ->
                    true
                previousItem.userId != currentItem.userId ->
                    true
                !isSameDay(previousItem.createdAt, currentItem.createdAt) ->
                    true
                else -> false
            }
        } else {
            false
        }
    }

    override fun isLast(position: Int): Boolean {
        val currentItem = getItem(position)
        val nextItem = getItem(position + 1)
        return when {
            currentItem == null ->
                false
            nextItem == null ->
                true
            currentItem.type == MessageCategory.SYSTEM_CONVERSATION.name ->
                true
            nextItem.type == MessageCategory.SYSTEM_CONVERSATION.name ->
                true
            nextItem.userId != currentItem.userId ->
                true
            !isSameDay(nextItem.createdAt, currentItem.createdAt) ->
                true
            else -> false
        }
    }

    override fun getItemCount(): Int {
        return data.size + if (hasBottomView && isSecret) {
            2
        } else if (hasBottomView || isSecret) {
            1
        } else {
            0
        }
    }

    override fun getItemViewType(position: Int): Int = getItemType(getItem(position))

    fun isRepresentative(messageItem: MessageItem): Boolean {
        return isBot && recipient?.userId != messageItem.userId && messageItem.userId != Session.getAccountId()
    }

    // Select message logic
    val selectSet = arraySetOf<MessageItem>()

    fun isSelect(position: Int): Boolean {
        return if (selectSet.isEmpty()) {
            false
        } else {
            selectSet.find { it.messageId == getItem(position)?.messageId } != null
        }
    }

    fun addSelect(messageItem: MessageItem): Boolean {
        return selectSet.add(messageItem)
    }

    fun removeSelect(messageItem: MessageItem): Boolean {
        return selectSet.remove(selectSet.find { it.messageId == messageItem.messageId })
    }

    @SuppressLint("NotifyDataSetChanged")
    fun markRead() {
        if (unreadMessageId != null) {
            unreadMessageId = null
            notifyDataSetChanged()
        }
    }

    var hasBottomView = false
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        getItem(holder.layoutPosition)?.let { messageItem ->
            (holder as BaseViewHolder).listen(holder.itemView, messageItem.messageId)
            if (holder is BaseMentionHolder) {
                holder.onViewAttachedToWindow()
            }
            if (holder is Terminable) {
                holder.onRead(messageItem.messageId, messageItem.expireIn, messageItem.expireAt)
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        (holder as BaseViewHolder).stopListen()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun refreshData(data: List<MessageItem>) {
        this.data.clear()
        this.data.addAll(data)
        notifyDataSetChanged()
    }

    // Time header
    override fun getHeaderId(position: Int): Long = getItem(position).notNullWithElse(
        {
            abs(it.createdAt.hashForDate())
        },
        0,
    )

    override fun onCreateHeaderViewHolder(parent: ViewGroup): TimeHolder =
        TimeHolder(ItemChatTimeBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindHeaderViewHolder(holder: TimeHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it.createdAt)
        }
    }

    override fun onCreateAttach(parent: ViewGroup): View =
        LayoutInflater.from(parent.context).inflate(R.layout.item_chat_unread, parent, false)

    override fun hasAttachView(position: Int): Boolean = if (unreadMessageId != null) {
        getItem(position)?.messageId == unreadMessageId
    } else {
        false
    }
    override fun onBindAttachView(view: View) {
        unreadMessageId?.let {
            ItemChatUnreadBinding.bind(view).unreadTv.text = view.context.getString(R.string.Unread_messages)
        }
    }

    // Data control
    fun submitNext(list: List<MessageItem>) {
        val size = data.size
        data.append(list)
        notifyItemRangeInserted(size, list.count())
    }

    fun submitPrevious(list: List<MessageItem>) {
        data.prepend(list)
        notifyItemRangeInserted(0, list.count())
    }

    fun insert(list: List<MessageItem>) {
        val position = layoutPosition(itemCount - 1)
        data.append(list)
        notifyItemChanged(position) // change last holder background
        notifyItemRangeInserted(position + 1, list.count())
    }

    fun update(list: List<MessageItem>) {
        list.forEach { item ->
            val index = data.indexOfFirst { it?.messageId == item.messageId }
            if (index != -1) {
                data.update(index, item)
                notifyItemChanged(layoutPosition(index))
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun delete(list: List<String>) {
        list.mapNotNull { id ->
            data.indexOfFirst { item -> id == item?.messageId }.takeIf { it != -1 }
        }.sortedDescending().forEach { p ->
            data.deleteByPosition(p)
            notifyDataSetChanged()
        }
    }

    fun indexMessage(messageId: String): Int {
        return data.indexOfFirst { messageItem -> messageItem?.messageId == messageId }
    }

    open class OnItemListener {

        open fun onSelect(isSelect: Boolean, messageItem: MessageItem, position: Int) {}

        open fun onLongClick(messageItem: MessageItem, position: Int): Boolean = true

        open fun onImageClick(messageItem: MessageItem, view: View) {}

        open fun onStickerClick(messageItem: MessageItem) {}

        open fun onFileClick(messageItem: MessageItem) {}

        open fun onAudioFileClick(messageItem: MessageItem) {}

        open fun onCancel(id: String) {}

        open fun onRetryUpload(messageId: String) {}

        open fun onRetryDownload(messageId: String) {}

        open fun onUserClick(userId: String) {}

        open fun onMentionClick(identityNumber: String) {}

        open fun onPhoneClick(phoneNumber: String) {}

        open fun onEmailClick(email: String) {}

        open fun onUrlClick(url: String) {}

        open fun onUrlLongClick(url: String) {}

        open fun onAddClick() {}

        open fun onBlockClick() {}

        open fun onActionClick(action: String, userId: String) {}

        open fun onAppCardClick(appCard: AppCardData, userId: String) {}

        open fun onAudioClick(messageItem: MessageItem) {}

        open fun onBillClick(messageItem: MessageItem) {}

        open fun onContactCardClick(userId: String) {}

        open fun onTransferClick(userId: String) {}

        open fun onQuoteMessageClick(messageId: String, quoteMessageId: String?) {}

        open fun onCallClick(messageItem: MessageItem) {}

        open fun onPostClick(view: View, messageItem: MessageItem) {}

        open fun onOpenHomePage() {}

        open fun onSayHi() {}

        open fun onLocationClick(messageItem: MessageItem) {}

        open fun onTextDoubleClick(messageItem: MessageItem) {}

        open fun onTranscriptClick(messageItem: MessageItem) {}
    }

    companion object {
        const val NULL_TYPE = 99
        const val UNKNOWN_TYPE = 0
        const val TEXT_TYPE = 1
        const val TEXT_QUOTE_TYPE = -1
        const val IMAGE_TYPE = 2
        const val IMAGE_QUOTE_TYPE = -2
        const val LINK_TYPE = 3
        const val VIDEO_TYPE = 4
        const val VIDEO_QUOTE_TYPE = -4
        const val AUDIO_TYPE = 5
        const val AUDIO_QUOTE_TYPE = -5
        const val FILE_TYPE = 6
        const val FILE_QUOTE_TYPE = -6
        const val STICKER_TYPE = 7
        const val CONTACT_CARD_TYPE = 8
        const val CONTACT_CARD_QUOTE_TYPE = -8
        const val CARD_TYPE = 9
        const val SNAPSHOT_TYPE = 10
        const val POST_TYPE = 11
        const val ACTION_TYPE = 12
        const val ACTION_CARD_TYPE = 13
        const val SYSTEM_TYPE = 14
        const val WAITING_TYPE = 15
        const val STRANGER_TYPE = 16
        const val SECRET_TYPE = 17
        const val CALL_TYPE = 18
        const val RECALL_TYPE = 19
        const val LOCATION_TYPE = 20
        const val GROUP_CALL_TYPE = 21
        const val TRANSCRIPT_TYPE = 22
        const val PIN_TYPE = 23
    }
}
