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
import one.mixin.android.databinding.ItemChatActionsCardBinding
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
import one.mixin.android.databinding.ItemChatImageCaptionBinding
import one.mixin.android.databinding.ItemChatImageQuoteBinding
import one.mixin.android.databinding.ItemChatLocationBinding
import one.mixin.android.databinding.ItemChatPostBinding
import one.mixin.android.databinding.ItemChatRecallBinding
import one.mixin.android.databinding.ItemChatSafeInscriptionBinding
import one.mixin.android.databinding.ItemChatSafeSnapshotBinding
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
import one.mixin.android.ui.conversation.holder.ActionsCardHolder
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
import one.mixin.android.ui.conversation.holder.ImageCaptionHolder
import one.mixin.android.ui.conversation.holder.ImageHolder
import one.mixin.android.ui.conversation.holder.ImageQuoteHolder
import one.mixin.android.ui.conversation.holder.LocationHolder
import one.mixin.android.ui.conversation.holder.PinMessageHolder
import one.mixin.android.ui.conversation.holder.PostHolder
import one.mixin.android.ui.conversation.holder.RecallHolder
import one.mixin.android.ui.conversation.holder.SafeInscriptionHolder
import one.mixin.android.ui.conversation.holder.SafeSnapshotHolder
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
import one.mixin.android.vo.Plan
import one.mixin.android.vo.User
import one.mixin.android.vo.create
import one.mixin.android.vo.isAppCard
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
    val onItemListener: OnItemListener,
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
            TEXT_TYPE -> {
                TextHolder(ItemChatTextBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }

            TEXT_QUOTE_TYPE -> {
                TextQuoteHolder(ItemChatTextQuoteBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }

            POST_TYPE -> {
                PostHolder(ItemChatPostBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }

            IMAGE_TYPE -> {
                ImageHolder(ItemChatImageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }

            IMAGE_QUOTE_TYPE -> {
                ImageQuoteHolder(ItemChatImageQuoteBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }

            IMAGE_CAPTION_TYPE -> {
                ImageCaptionHolder(ItemChatImageCaptionBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }

            SYSTEM_TYPE -> {
                SystemHolder(ItemChatSystemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }

            CARD_TYPE -> {
                CardHolder(ItemChatCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }

            SNAPSHOT_TYPE -> {
                SnapshotHolder(ItemChatSnapshotBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }

            SAFE_SNAPSHOT_TYPE -> {
                SafeSnapshotHolder(ItemChatSafeSnapshotBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }

            SYSTEM_SAFE_INSCRIPTION_TYPE -> {
                SafeInscriptionHolder(ItemChatSafeInscriptionBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }

            WAITING_TYPE -> {
                WaitingHolder(ItemChatWaitingBinding.inflate(LayoutInflater.from(parent.context), parent, false), onItemListener)
            }

            STRANGER_TYPE -> {
                StrangerHolder(ItemChatStrangerBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }

            UNKNOWN_TYPE -> {
                UnknownHolder(ItemChatUnknownBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }

            FILE_TYPE -> {
                FileHolder(ItemChatFileBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }

            FILE_QUOTE_TYPE -> {
                FileQuoteHolder(ItemChatFileQuoteBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }

            AUDIO_TYPE -> {
                AudioHolder(ItemChatAudioBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }

            AUDIO_QUOTE_TYPE -> {
                AudioQuoteHolder(ItemChatAudioQuoteBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }

            STICKER_TYPE -> {
                StickerHolder(ItemChatStickerBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }

            ACTION_TYPE -> {
                ActionHolder(ItemChatActionBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }

            ACTION_CARD_TYPE -> {
                ActionCardHolder(ItemChatActionCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }

            ACTIONS_CARD_TYPE -> {
                ActionsCardHolder(ItemChatActionsCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }

            LINK_TYPE -> {
                HyperlinkHolder(ItemChatHyperlinkBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }

            CONTACT_CARD_TYPE -> {
                ContactCardHolder(ItemChatContactCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }

            CONTACT_CARD_QUOTE_TYPE -> {
                ContactCardQuoteHolder(ItemChatContactCardQuoteBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }

            VIDEO_TYPE -> {
                VideoHolder(ItemChatVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }

            VIDEO_QUOTE_TYPE -> {
                VideoQuoteHolder(ItemChatVideoQuoteBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }

            SECRET_TYPE -> {
                SecretHolder(ItemChatSecretBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }

            CALL_TYPE -> {
                CallHolder(ItemChatCallBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }

            RECALL_TYPE -> {
                RecallHolder(ItemChatRecallBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }

            LOCATION_TYPE -> {
                LocationHolder(ItemChatLocationBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }

            GROUP_CALL_TYPE -> {
                GroupCallHolder(ItemChatSystemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }

            TRANSCRIPT_TYPE -> {
                TranscriptHolder(ItemChatTranscriptBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }

            PIN_TYPE -> {
                PinMessageHolder(ItemChatSystemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }

            else -> {
                TransparentHolder(ItemChatTransparentBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
        }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        getItem(position)?.let {
            when (getItemViewType(position)) {
                TEXT_TYPE -> {
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

                TEXT_QUOTE_TYPE -> {
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

                POST_TYPE -> {
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

                IMAGE_TYPE -> {
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

                IMAGE_QUOTE_TYPE -> {
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

                IMAGE_CAPTION_TYPE -> {
                    (holder as ImageCaptionHolder).bind(
                        it,
                        isLast(position),
                        isFirst(position),
                        selectSet.size > 0,
                        isSelect(position),
                        isRepresentative(it),
                        onItemListener
                    )
                }

                VIDEO_TYPE -> {
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

                VIDEO_QUOTE_TYPE -> {
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

                AUDIO_TYPE -> {
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

                AUDIO_QUOTE_TYPE -> {
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

                SYSTEM_TYPE -> {
                    (holder as SystemHolder).bind(
                        it,
                        selectSet.size > 0,
                        isSelect(position),
                        onItemListener,
                    )
                }

                CARD_TYPE -> {
                    (holder as CardHolder).bind(it)
                }

                SNAPSHOT_TYPE -> {
                    (holder as SnapshotHolder).bind(
                        it,
                        isLast(position),
                        selectSet.size > 0,
                        isSelect(position),
                        onItemListener,
                    )
                }

                SAFE_SNAPSHOT_TYPE -> {
                    (holder as SafeSnapshotHolder).bind(
                        it,
                        isLast(position),
                        selectSet.size > 0,
                        isSelect(position),
                        onItemListener,
                    )
                }

                SYSTEM_SAFE_INSCRIPTION_TYPE -> {
                    (holder as SafeInscriptionHolder).bind(
                        it,
                        isFirst(position),
                        isLast(position),
                        selectSet.size > 0,
                        isSelect(position),
                        isRepresentative(it),
                        onItemListener,
                    )
                }

                FILE_TYPE -> {
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

                FILE_QUOTE_TYPE -> {
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

                WAITING_TYPE -> {
                    (holder as WaitingHolder).bind(
                        it,
                        isLast(position),
                        isFirst(position),
                        onItemListener,
                    )
                }

                STRANGER_TYPE -> {
                    (holder as StrangerHolder).bind(onItemListener, isBot)
                }

                UNKNOWN_TYPE -> {
                    (holder as UnknownHolder).bind(
                        it,
                        isLast(position),
                        isFirst(position),
                        selectSet.size > 0,
                        isSelect(position),
                        onItemListener,
                    )
                }

                STICKER_TYPE -> {
                    (holder as StickerHolder).bind(
                        it,
                        isFirst(position),
                        selectSet.size > 0,
                        isSelect(position),
                        isRepresentative(it),
                        onItemListener,
                    )
                }

                LINK_TYPE -> {
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

                ACTION_TYPE -> {
                    (holder as ActionHolder).bind(
                        it,
                        isFirst(position),
                        selectSet.size > 0,
                        isSelect(position),
                        onItemListener,
                    )
                }

                ACTION_CARD_TYPE -> {
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

                ACTIONS_CARD_TYPE ->{
                    (holder as ActionsCardHolder).bind(
                        it,
                        isFirst(position),
                        isLast(position),
                        selectSet.size > 0,
                        isSelect(position),
                        isRepresentative(it),
                        onItemListener,
                    )
                }

                CONTACT_CARD_TYPE -> {
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

                CONTACT_CARD_QUOTE_TYPE -> {
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

                SECRET_TYPE -> {
                    (holder as SecretHolder).bind(onItemListener)
                }

                CALL_TYPE -> {
                    (holder as CallHolder).bind(
                        it,
                        isFirst(position),
                        selectSet.size > 0,
                        isSelect(position),
                        onItemListener,
                    )
                }

                RECALL_TYPE -> {
                    (holder as RecallHolder).bind(
                        it,
                        isFirst(position),
                        isLast(position),
                        selectSet.size > 0,
                        isSelect(position),
                        onItemListener,
                    )
                }

                LOCATION_TYPE -> {
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

                GROUP_CALL_TYPE -> {
                    (holder as GroupCallHolder).bind(
                        it,
                        selectSet.size > 0,
                        isSelect(position),
                        onItemListener,
                    )
                }

                TRANSCRIPT_TYPE -> {
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

                PIN_TYPE -> {
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
                    item.status == MessageStatus.UNKNOWN.name -> UNKNOWN_TYPE
                    item.type == MessageCategory.STRANGER.name -> STRANGER_TYPE
                    item.type == MessageCategory.SECRET.name -> SECRET_TYPE
                    item.status == MessageStatus.FAILED.name -> WAITING_TYPE
                    item.isText() -> {
                        if (!item.quoteId.isNullOrEmpty()) {
                            TEXT_QUOTE_TYPE
                        } else if (!item.siteName.isNullOrBlank() || !item.siteDescription.isNullOrBlank()) {
                            LINK_TYPE
                        } else {
                            TEXT_TYPE
                        }
                    }

                    item.isImage() -> {
                        if (!item.caption.isNullOrEmpty()) {
                            IMAGE_CAPTION_TYPE
                        } else if (!item.quoteId.isNullOrEmpty()) {
                            IMAGE_QUOTE_TYPE
                        } else {
                            IMAGE_TYPE
                        }
                    }

                    item.isSticker() -> STICKER_TYPE
                    item.isData() -> {
                        if (!item.quoteId.isNullOrEmpty()) {
                            FILE_QUOTE_TYPE
                        } else {
                            FILE_TYPE
                        }
                    }

                    item.type == MessageCategory.SYSTEM_CONVERSATION.name -> SYSTEM_TYPE
                    item.type == MessageCategory.SYSTEM_ACCOUNT_SNAPSHOT.name -> SNAPSHOT_TYPE
                    item.type == MessageCategory.SYSTEM_SAFE_INSCRIPTION.name -> SYSTEM_SAFE_INSCRIPTION_TYPE
                    item.type == MessageCategory.SYSTEM_SAFE_SNAPSHOT.name -> SAFE_SNAPSHOT_TYPE
                    item.type == MessageCategory.APP_BUTTON_GROUP.name -> ACTION_TYPE
                    item.type == MessageCategory.APP_CARD.name -> {
                        if (item.appCardData?.oldVersion == true) ACTION_CARD_TYPE else ACTIONS_CARD_TYPE
                    }


                    item.isContact() -> {
                        if (!item.quoteId.isNullOrEmpty()) {
                            CONTACT_CARD_QUOTE_TYPE
                        } else {
                            CONTACT_CARD_TYPE
                        }
                    }

                    item.isVideo() or item.isLive() -> {
                        if (!item.quoteId.isNullOrEmpty()) {
                            VIDEO_QUOTE_TYPE
                        } else {
                            VIDEO_TYPE
                        }
                    }

                    item.isAudio() -> {
                        if (!item.quoteId.isNullOrEmpty()) {
                            AUDIO_QUOTE_TYPE
                        } else {
                            AUDIO_TYPE
                        }
                    }

                    item.isPost() -> POST_TYPE
                    item.isCallMessage() -> CALL_TYPE
                    item.isRecall() -> RECALL_TYPE
                    item.isLocation() -> LOCATION_TYPE
                    item.isGroupCall() -> GROUP_CALL_TYPE
                    item.isTranscript() -> TRANSCRIPT_TYPE
                    item.isPin() -> PIN_TYPE
                    else -> UNKNOWN_TYPE
                }
            },
            NULL_TYPE,
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
                itemCount - 1 ->
                    create(
                        MessageCategory.STRANGER.name,
                        if (data.size > 0) {
                            data.last()?.createdAt
                        } else {
                            null
                        },
                    )

                0 ->
                    create(
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

    override fun isButtonGroup(position: Int): Boolean {
        val currentItem = getItem(position)
        return currentItem?.isAppCard() == true && !currentItem.appCardData?.actions.isNullOrEmpty()
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
        return data.size +
            if (hasBottomView && isSecret) {
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
    override fun getHeaderId(position: Int): Long =
        getItem(position).notNullWithElse(
            {
                abs(it.createdAt.hashForDate())
            },
            0,
        )

    override fun onCreateHeaderViewHolder(parent: ViewGroup): TimeHolder =
        TimeHolder(ItemChatTimeBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindHeaderViewHolder(
        holder: TimeHolder,
        position: Int,
    ) {
        getItem(position)?.let {
            holder.bind(it.createdAt)
        }
    }

    override fun onCreateAttach(parent: ViewGroup): View =
        LayoutInflater.from(parent.context).inflate(R.layout.item_chat_unread, parent, false)

    override fun hasAttachView(position: Int): Boolean =
        if (unreadMessageId != null) {
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
        open fun onSelect(
            isSelect: Boolean,
            messageItem: MessageItem,
            position: Int,
        ) {}

        open fun onLongClick(
            messageItem: MessageItem,
            position: Int,
        ): Boolean = true

        open fun onImageClick(
            messageItem: MessageItem,
            view: View,
        ) {}

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

        open fun onInscriptionClick(
            conversationId: String,
            messageId: String,
            assetId: String?,
            inscriptionHash: String?,
            snapshotId: String?,
        ) {}

        open fun onActionClick(
            action: String,
            userId: String,
            appId: String? = null
        ) {}

        open fun onAppCardClick(
            appCard: AppCardData,
            userId: String,
        ) {}

        open fun onAudioClick(messageItem: MessageItem) {}

        open fun onBillClick(messageItem: MessageItem) {}

        open fun onContactCardClick(userId: String) {}

        open fun onTransferClick(userId: String) {}

        open fun onQuoteMessageClick(
            messageId: String,
            quoteMessageId: String?,
        ) {}

        open fun onCallClick(messageItem: MessageItem) {}

        open fun onPostClick(
            view: View,
            messageItem: MessageItem,
        ) {}

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
        const val IMAGE_CAPTION_TYPE = 3
        const val LINK_TYPE = 4
        const val VIDEO_TYPE = 5
        const val VIDEO_QUOTE_TYPE = -5
        const val AUDIO_TYPE = 6
        const val AUDIO_QUOTE_TYPE = -6
        const val FILE_TYPE = 7
        const val FILE_QUOTE_TYPE = -7
        const val STICKER_TYPE = 8
        const val CONTACT_CARD_TYPE = 9
        const val CONTACT_CARD_QUOTE_TYPE = -9
        const val CARD_TYPE = 10
        const val SNAPSHOT_TYPE = 11
        const val SAFE_SNAPSHOT_TYPE = 12
        const val SYSTEM_SAFE_INSCRIPTION_TYPE = 13
        const val POST_TYPE = 14
        const val ACTION_TYPE = 15
        const val ACTION_CARD_TYPE = 16
        const val ACTIONS_CARD_TYPE = 17
        const val SYSTEM_TYPE = 18
        const val WAITING_TYPE = 19
        const val STRANGER_TYPE = 20
        const val SECRET_TYPE = 21
        const val CALL_TYPE = 22
        const val RECALL_TYPE = 23
        const val LOCATION_TYPE = 24
        const val GROUP_CALL_TYPE = 25
        const val TRANSCRIPT_TYPE = 26
        const val PIN_TYPE = 27
    }
}
