package one.mixin.android.ui.conversation.chat

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
import one.mixin.android.db.fetcher.MessageFetcher
import one.mixin.android.db.fetcher.MessageFetcher.Companion.SCROLL_THRESHOLD
import one.mixin.android.extension.hashForDate
import one.mixin.android.extension.isSameDay
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.session.Session
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
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
import timber.log.Timber
import kotlin.math.abs

class MessageAdapter(
    val data: CompressedList<MessageItem>,
    private val miniMarkwon: Markwon,
    val onItemListener: ConversationAdapter.OnItemListener,
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
            ConversationAdapter.TEXT_TYPE -> {
                TextHolder(ItemChatTextBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            ConversationAdapter.TEXT_QUOTE_TYPE -> {
                TextQuoteHolder(ItemChatTextQuoteBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            ConversationAdapter.POST_TYPE -> {
                PostHolder(ItemChatPostBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            ConversationAdapter.IMAGE_TYPE -> {
                ImageHolder(ItemChatImageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            ConversationAdapter.IMAGE_QUOTE_TYPE -> {
                ImageQuoteHolder(ItemChatImageQuoteBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            ConversationAdapter.SYSTEM_TYPE -> {
                SystemHolder(ItemChatSystemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            ConversationAdapter.CARD_TYPE -> {
                CardHolder(ItemChatCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            ConversationAdapter.SNAPSHOT_TYPE -> {
                SnapshotHolder(ItemChatSnapshotBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            ConversationAdapter.WAITING_TYPE -> {
                WaitingHolder(ItemChatWaitingBinding.inflate(LayoutInflater.from(parent.context), parent, false), onItemListener)
            }
            ConversationAdapter.STRANGER_TYPE -> {
                StrangerHolder(ItemChatStrangerBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            ConversationAdapter.UNKNOWN_TYPE -> {
                UnknownHolder(ItemChatUnknownBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            ConversationAdapter.FILE_TYPE -> {
                FileHolder(ItemChatFileBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            ConversationAdapter.FILE_QUOTE_TYPE -> {
                FileQuoteHolder(ItemChatFileQuoteBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            ConversationAdapter.AUDIO_TYPE -> {
                AudioHolder(ItemChatAudioBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            ConversationAdapter.AUDIO_QUOTE_TYPE -> {
                AudioQuoteHolder(ItemChatAudioQuoteBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            ConversationAdapter.STICKER_TYPE -> {
                StickerHolder(ItemChatStickerBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            ConversationAdapter.ACTION_TYPE -> {
                ActionHolder(ItemChatActionBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            ConversationAdapter.ACTION_CARD_TYPE -> {
                ActionCardHolder(ItemChatActionCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            ConversationAdapter.LINK_TYPE -> {
                HyperlinkHolder(ItemChatHyperlinkBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            ConversationAdapter.CONTACT_CARD_TYPE -> {
                ContactCardHolder(ItemChatContactCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            ConversationAdapter.CONTACT_CARD_QUOTE_TYPE -> {
                ContactCardQuoteHolder(ItemChatContactCardQuoteBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            ConversationAdapter.VIDEO_TYPE -> {
                VideoHolder(ItemChatVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            ConversationAdapter.VIDEO_QUOTE_TYPE -> {
                VideoQuoteHolder(ItemChatVideoQuoteBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            ConversationAdapter.SECRET_TYPE -> {
                SecretHolder(ItemChatSecretBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            ConversationAdapter.CALL_TYPE -> {
                CallHolder(ItemChatCallBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            ConversationAdapter.RECALL_TYPE -> {
                RecallHolder(ItemChatRecallBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            ConversationAdapter.LOCATION_TYPE -> {
                LocationHolder(ItemChatLocationBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            ConversationAdapter.GROUP_CALL_TYPE -> {
                GroupCallHolder(ItemChatSystemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            ConversationAdapter.TRANSCRIPT_TYPE -> {
                TranscriptHolder(ItemChatTranscriptBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            ConversationAdapter.PIN_TYPE -> {
                PinMessageHolder(ItemChatSystemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            else -> {
                TransparentHolder(ItemChatTransparentBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        getItem(position)?.let {
            when (getItemViewType(position)) {
                ConversationAdapter.TEXT_TYPE -> {
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
                ConversationAdapter.TEXT_QUOTE_TYPE -> {
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
                ConversationAdapter.POST_TYPE -> {
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
                ConversationAdapter.IMAGE_TYPE -> {
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
                ConversationAdapter.IMAGE_QUOTE_TYPE -> {
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
                ConversationAdapter.VIDEO_TYPE -> {
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
                ConversationAdapter.VIDEO_QUOTE_TYPE -> {
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
                ConversationAdapter.AUDIO_TYPE -> {
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
                ConversationAdapter.AUDIO_QUOTE_TYPE -> {
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
                ConversationAdapter.SYSTEM_TYPE -> {
                    (holder as SystemHolder).bind(
                        it,
                        selectSet.size > 0,
                        isSelect(position),
                        onItemListener,
                    )
                }
                ConversationAdapter.CARD_TYPE -> {
                    (holder as CardHolder).bind(it)
                }
                ConversationAdapter.SNAPSHOT_TYPE -> {
                    (holder as SnapshotHolder).bind(
                        it,
                        isLast(position),
                        selectSet.size > 0,
                        isSelect(position),
                        onItemListener,
                    )
                }
                ConversationAdapter.FILE_TYPE -> {
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
                ConversationAdapter.FILE_QUOTE_TYPE -> {
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
                ConversationAdapter.WAITING_TYPE -> {
                    (holder as WaitingHolder).bind(
                        it,
                        isLast(position),
                        isFirst(position),
                        onItemListener,
                    )
                }
                ConversationAdapter.STRANGER_TYPE -> {
                    (holder as StrangerHolder).bind(onItemListener, isBot)
                }
                ConversationAdapter.UNKNOWN_TYPE -> {
                    (holder as UnknownHolder).bind(
                        it,
                        isLast(position),
                        isFirst(position),
                        selectSet.size > 0,
                        isSelect(position),
                        onItemListener,
                    )
                }
                ConversationAdapter.STICKER_TYPE -> {
                    (holder as StickerHolder).bind(
                        it,
                        isFirst(position),
                        selectSet.size > 0,
                        isSelect(position),
                        isRepresentative(it),
                        onItemListener,
                    )
                }
                ConversationAdapter.LINK_TYPE -> {
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
                ConversationAdapter.ACTION_TYPE -> {
                    (holder as ActionHolder).bind(
                        it,
                        isFirst(position),
                        selectSet.size > 0,
                        isSelect(position),
                        onItemListener,
                    )
                }
                ConversationAdapter.ACTION_CARD_TYPE -> {
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
                ConversationAdapter.CONTACT_CARD_TYPE -> {
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
                ConversationAdapter.CONTACT_CARD_QUOTE_TYPE -> {
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
                ConversationAdapter.SECRET_TYPE -> {
                    (holder as SecretHolder).bind(onItemListener)
                }
                ConversationAdapter.CALL_TYPE -> {
                    (holder as CallHolder).bind(
                        it,
                        isFirst(position),
                        selectSet.size > 0,
                        isSelect(position),
                        onItemListener,
                    )
                }
                ConversationAdapter.RECALL_TYPE -> {
                    (holder as RecallHolder).bind(
                        it,
                        isFirst(position),
                        isLast(position),
                        selectSet.size > 0,
                        isSelect(position),
                        onItemListener,
                    )
                }
                ConversationAdapter.LOCATION_TYPE -> {
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
                ConversationAdapter.GROUP_CALL_TYPE -> {
                    (holder as GroupCallHolder).bind(
                        it,
                        selectSet.size > 0,
                        isSelect(position),
                        onItemListener,
                    )
                }
                ConversationAdapter.TRANSCRIPT_TYPE -> {
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
                ConversationAdapter.PIN_TYPE -> {
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
                    item.status == MessageStatus.UNKNOWN.name -> ConversationAdapter.UNKNOWN_TYPE
                    item.type == MessageCategory.STRANGER.name -> ConversationAdapter.STRANGER_TYPE
                    item.type == MessageCategory.SECRET.name -> ConversationAdapter.SECRET_TYPE
                    item.status == MessageStatus.FAILED.name -> ConversationAdapter.WAITING_TYPE
                    item.isText() -> {
                        if (!item.quoteId.isNullOrEmpty()) {
                            ConversationAdapter.TEXT_QUOTE_TYPE
                        } else if (!item.siteName.isNullOrBlank() || !item.siteDescription.isNullOrBlank()) {
                            ConversationAdapter.LINK_TYPE
                        } else {
                            ConversationAdapter.TEXT_TYPE
                        }
                    }
                    item.isImage() -> {
                        if (!item.quoteId.isNullOrEmpty()) {
                            ConversationAdapter.IMAGE_QUOTE_TYPE
                        } else {
                            ConversationAdapter.IMAGE_TYPE
                        }
                    }
                    item.isSticker() -> ConversationAdapter.STICKER_TYPE
                    item.isData() -> {
                        if (!item.quoteId.isNullOrEmpty()) {
                            ConversationAdapter.FILE_QUOTE_TYPE
                        } else {
                            ConversationAdapter.FILE_TYPE
                        }
                    }
                    item.type == MessageCategory.SYSTEM_CONVERSATION.name -> ConversationAdapter.SYSTEM_TYPE
                    item.type == MessageCategory.SYSTEM_ACCOUNT_SNAPSHOT.name -> ConversationAdapter.SNAPSHOT_TYPE
                    item.type == MessageCategory.APP_BUTTON_GROUP.name -> ConversationAdapter.ACTION_TYPE
                    item.type == MessageCategory.APP_CARD.name -> ConversationAdapter.ACTION_CARD_TYPE
                    item.isContact() -> {
                        if (!item.quoteId.isNullOrEmpty()) {
                            ConversationAdapter.CONTACT_CARD_QUOTE_TYPE
                        } else {
                            ConversationAdapter.CONTACT_CARD_TYPE
                        }
                    }
                    item.isVideo() or item.isLive() -> {
                        if (!item.quoteId.isNullOrEmpty()) {
                            ConversationAdapter.VIDEO_QUOTE_TYPE
                        } else {
                            ConversationAdapter.VIDEO_TYPE
                        }
                    }
                    item.isAudio() -> {
                        if (!item.quoteId.isNullOrEmpty()) {
                            ConversationAdapter.AUDIO_QUOTE_TYPE
                        } else {
                            ConversationAdapter.AUDIO_TYPE
                        }
                    }
                    item.isPost() -> ConversationAdapter.POST_TYPE
                    item.isCallMessage() -> ConversationAdapter.CALL_TYPE
                    item.isRecall() -> ConversationAdapter.RECALL_TYPE
                    item.isLocation() -> ConversationAdapter.LOCATION_TYPE
                    item.isGroupCall() -> ConversationAdapter.GROUP_CALL_TYPE
                    item.isTranscript() -> ConversationAdapter.TRANSCRIPT_TYPE
                    item.isPin() -> ConversationAdapter.PIN_TYPE
                    else -> ConversationAdapter.UNKNOWN_TYPE
                }
            },
            ConversationAdapter.NULL_TYPE,
        )
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
        if (position < itemCount - 1 && position >= 0) {
            if (position < SCROLL_THRESHOLD) {
                data.first()?.messageId?.let { id ->
                    previousPage(id)
                }
            } else if (position > data.size - SCROLL_THRESHOLD - 1) {
                data.last()?.messageId?.let { id ->
                    nextPage(id)
                }
            }
            return data[position]
        } else {
            return null
        }
    }
    override fun isListLast(position: Int): Boolean {
        return position == itemCount - 1
    }

    fun isRepresentative(messageItem: MessageItem): Boolean {
        return isBot && recipient?.userId != messageItem.userId && messageItem.userId != Session.getAccountId()
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

    fun markRead() {
        unreadMessageId = null
        notifyDataSetChanged()
    }

    var hasBottomView = false
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
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
        val size = data.size
        data.append(list)
        notifyItemRangeInserted(size + 1, list.count())
    }

    fun update(list: List<MessageItem>) {
        list.forEach { item ->
            val index = data.indexOfFirst { it?.messageId == item.messageId }
            if (index != -1) {
                data.update(index, item)
                notifyItemChanged(index + 1)
            }
        }
    }

    fun delete(list: List<String>) {
        list.mapNotNull { id ->
            data.indexOfFirst { item -> id == item?.messageId }.takeIf { it != -1 }
        }.sortedDescending().forEach { p ->
            data.deleteByPosition(p)
            Timber.e("Removed $p")
            notifyItemRemoved(p)
        }
    }

    fun indexMessage(messageId: String): Int {
        return data.indexOfFirst { messageItem -> messageItem?.messageId == messageId }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun refreshData(data: List<MessageItem>) {
        this.data.clear()
        this.data.addAll(data)
        notifyDataSetChanged()
    }

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
}
