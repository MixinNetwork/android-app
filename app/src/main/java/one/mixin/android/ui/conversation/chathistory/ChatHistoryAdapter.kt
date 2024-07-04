package one.mixin.android.ui.conversation.chathistory

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatActionBinding
import one.mixin.android.databinding.ItemChatActionCardBinding
import one.mixin.android.databinding.ItemChatAudioBinding
import one.mixin.android.databinding.ItemChatAudioQuoteBinding
import one.mixin.android.databinding.ItemChatActionsCardBinding
import one.mixin.android.databinding.ItemChatContactCardBinding
import one.mixin.android.databinding.ItemChatContactCardQuoteBinding
import one.mixin.android.databinding.ItemChatFileBinding
import one.mixin.android.databinding.ItemChatFileQuoteBinding
import one.mixin.android.databinding.ItemChatImageBinding
import one.mixin.android.databinding.ItemChatImageQuoteBinding
import one.mixin.android.databinding.ItemChatLocationBinding
import one.mixin.android.databinding.ItemChatPostBinding
import one.mixin.android.databinding.ItemChatRecallBinding
import one.mixin.android.databinding.ItemChatStickerBinding
import one.mixin.android.databinding.ItemChatTextBinding
import one.mixin.android.databinding.ItemChatTextQuoteBinding
import one.mixin.android.databinding.ItemChatTimeBinding
import one.mixin.android.databinding.ItemChatTranscriptBinding
import one.mixin.android.databinding.ItemChatTransparentBinding
import one.mixin.android.databinding.ItemChatUnknownBinding
import one.mixin.android.databinding.ItemChatVideoBinding
import one.mixin.android.databinding.ItemChatVideoQuoteBinding
import one.mixin.android.extension.hashForDate
import one.mixin.android.extension.isSameDay
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.ui.common.recyclerview.SafePagedListAdapter
import one.mixin.android.ui.conversation.chathistory.holder.ActionCardHolder
import one.mixin.android.ui.conversation.chathistory.holder.ActionHolder
import one.mixin.android.ui.conversation.chathistory.holder.AudioHolder
import one.mixin.android.ui.conversation.chathistory.holder.AudioQuoteHolder
import one.mixin.android.ui.conversation.chathistory.holder.BaseViewHolder
import one.mixin.android.ui.conversation.chathistory.holder.ContactCardHolder
import one.mixin.android.ui.conversation.chathistory.holder.ContactCardQuoteHolder
import one.mixin.android.ui.conversation.chathistory.holder.FileHolder
import one.mixin.android.ui.conversation.chathistory.holder.FileQuoteHolder
import one.mixin.android.ui.conversation.chathistory.holder.ImageHolder
import one.mixin.android.ui.conversation.chathistory.holder.ImageQuoteHolder
import one.mixin.android.ui.conversation.chathistory.holder.LocationHolder
import one.mixin.android.ui.conversation.chathistory.holder.PostHolder
import one.mixin.android.ui.conversation.chathistory.holder.RecallHolder
import one.mixin.android.ui.conversation.chathistory.holder.StickerHolder
import one.mixin.android.ui.conversation.chathistory.holder.TextHolder
import one.mixin.android.ui.conversation.chathistory.holder.TextQuoteHolder
import one.mixin.android.ui.conversation.chathistory.holder.TranscriptHolder
import one.mixin.android.ui.conversation.chathistory.holder.TransparentHolder
import one.mixin.android.ui.conversation.chathistory.holder.UnknownHolder
import one.mixin.android.ui.conversation.chathistory.holder.VideoHolder
import one.mixin.android.ui.conversation.chathistory.holder.VideoQuoteHolder
import one.mixin.android.ui.conversation.holder.TimeHolder
import one.mixin.android.util.markdown.MarkwonUtil
import one.mixin.android.vo.AppCardData
import one.mixin.android.vo.ChatHistoryMessageItem
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.isAppButtonGroup
import one.mixin.android.vo.isAppCard
import one.mixin.android.vo.isAudio
import one.mixin.android.vo.isContact
import one.mixin.android.vo.isData
import one.mixin.android.vo.isImage
import one.mixin.android.vo.isLive
import one.mixin.android.vo.isLocation
import one.mixin.android.vo.isPost
import one.mixin.android.vo.isRecall
import one.mixin.android.vo.isSticker
import one.mixin.android.vo.isText
import one.mixin.android.vo.isTranscript
import one.mixin.android.vo.isVideo
import one.mixin.android.widget.MixinStickyRecyclerHeadersAdapter
import kotlin.math.abs

class ChatHistoryAdapter(
    private val onItemListener: OnItemListener,
    private val context: Activity,
) : SafePagedListAdapter<ChatHistoryMessageItem, BaseViewHolder>(ChatHistoryMessageItem.DIFF_CALLBACK),
    MixinStickyRecyclerHeadersAdapter<TimeHolder> {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): BaseViewHolder {
        return when (viewType) {
            1 ->
                TextHolder(
                    ItemChatTextBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false,
                    ),
                )
            -1 ->
                TextQuoteHolder(
                    ItemChatTextQuoteBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false,
                    ),
                )
            2 ->
                ImageHolder(
                    ItemChatImageBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false,
                    ),
                )
            -2 ->
                ImageQuoteHolder(
                    ItemChatImageQuoteBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false,
                    ),
                )
            3 ->
                VideoHolder(
                    ItemChatVideoBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false,
                    ),
                )
            -3 ->
                VideoQuoteHolder(
                    ItemChatVideoQuoteBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false,
                    ),
                )
            4 ->
                FileHolder(
                    ItemChatFileBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false,
                    ),
                )
            -4 ->
                FileQuoteHolder(
                    ItemChatFileQuoteBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false,
                    ),
                )
            5 ->
                AudioHolder(
                    ItemChatAudioBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false,
                    ),
                )
            -5 ->
                AudioQuoteHolder(
                    ItemChatAudioQuoteBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false,
                    ),
                )
            6 ->
                ContactCardHolder(
                    ItemChatContactCardBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false,
                    ),
                )
            -6 ->
                ContactCardQuoteHolder(
                    ItemChatContactCardQuoteBinding.inflate(
                        LayoutInflater.from(
                            parent.context,
                        ),
                        parent,
                        false,
                    ),
                )
            7 ->
                StickerHolder(
                    ItemChatStickerBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false,
                    ),
                )
            8 ->
                TranscriptHolder(
                    ItemChatTranscriptBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false,
                    ),
                )
            9 ->
                LocationHolder(
                    ItemChatLocationBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false,
                    ),
                )
            10 ->
                ActionCardHolder(
                    ItemChatActionCardBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false,
                    ),
                )
            11 ->
                ActionsCardHolder(
                    ItemChatActionsCardBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false,
                    ),
                )
            12 ->
                PostHolder(
                    ItemChatPostBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false,
                    ),
                )
            13 ->
                TranscriptHolder(
                    ItemChatTranscriptBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false,
                    ),
                )
            14 ->
                RecallHolder(
                    ItemChatRecallBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false,
                    ),
                )
            15 ->
                ActionHolder(
                    ItemChatActionBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false,
                    ),
                )
            0 ->
                TransparentHolder(
                    ItemChatTransparentBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false,
                    ),
                )
            else ->
                UnknownHolder(
                    ItemChatUnknownBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false,
                    ),
                )
        }
    }

    private val miniMarkwon by lazy {
        MarkwonUtil.getMiniMarkwon(context)
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder,
        position: Int,
    ) {
        val transcript = getItem(position) ?: return

        return when (getItemViewType(position)) {
            1 ->
                (holder as TextHolder).bind(
                    transcript,
                    isLast = isLast(position),
                    isFirst = isFirst(position),
                    onItemListener,
                )
            -1 ->
                (holder as TextQuoteHolder).bind(
                    transcript,
                    isLast = isLast(position),
                    isFirst = isFirst(position),
                    onItemListener,
                )
            2 ->
                (holder as ImageHolder).bind(
                    transcript,
                    isLast = isLast(position),
                    isFirst = isFirst(position),
                    onItemListener,
                )
            -2 ->
                (holder as ImageQuoteHolder).bind(
                    transcript,
                    isLast = isLast(position),
                    isFirst = isFirst(position),
                    onItemListener,
                )
            3 ->
                (holder as VideoHolder).bind(
                    transcript,
                    isLast = isLast(position),
                    isFirst = isFirst(position),
                    onItemListener,
                )
            -3 ->
                (holder as VideoQuoteHolder).bind(
                    transcript,
                    isLast = isLast(position),
                    isFirst = isFirst(position),
                    onItemListener,
                )
            4 ->
                (holder as FileHolder).bind(
                    transcript,
                    isLast = isLast(position),
                    isFirst = isFirst(position),
                    onItemListener,
                )
            -4 ->
                (holder as FileQuoteHolder).bind(
                    transcript,
                    isLast = isLast(position),
                    isFirst = isFirst(position),
                    onItemListener,
                )
            5 ->
                (holder as AudioHolder).bind(
                    transcript,
                    isLast = isLast(position),
                    isFirst = isFirst(position),
                    onItemListener,
                )
            -5 ->
                (holder as AudioQuoteHolder).bind(
                    transcript,
                    isLast = isLast(position),
                    isFirst = isFirst(position),
                    onItemListener,
                )
            6 ->
                (holder as ContactCardHolder).bind(
                    transcript,
                    isLast = isLast(position),
                    isFirst = isFirst(position),
                    onItemListener,
                )
            -6 ->
                (holder as ContactCardQuoteHolder).bind(
                    transcript,
                    isLast = isLast(position),
                    isFirst = isFirst(position),
                    onItemListener,
                )
            7 ->
                (holder as StickerHolder).bind(
                    transcript,
                    isFirst = isFirst(position),
                    onItemListener,
                )
            8 ->
                (holder as TranscriptHolder).bind(
                    transcript,
                    isLast = isLast(position),
                    isFirst = isFirst(position),
                    onItemListener,
                )
            9 ->
                (holder as LocationHolder).bind(
                    transcript,
                    isLast = isLast(position),
                    isFirst = isFirst(position),
                    onItemListener,
                )
            10 ->
                (holder as ActionCardHolder).bind(
                    transcript,
                    isLast = isLast(position),
                    isFirst = isFirst(position),
                    onItemListener,
                )
            11 ->
                (holder as ActionsCardHolder).bind(
                    transcript,
                    isLast = isLast(position),
                    isFirst = isFirst(position),
                    onItemListener,
                )
            12 ->
                (holder as PostHolder).bind(
                    transcript,
                    isLast = isLast(position),
                    isFirst = isFirst(position),
                    onItemListener,
                    miniMarkwon,
                )
            13 ->
                (holder as TranscriptHolder).bind(
                    transcript,
                    isLast = isLast(position),
                    isFirst = isFirst(position),
                    onItemListener,
                )
            14 ->
                (holder as RecallHolder).bind(
                    transcript,
                    isFirst = isFirst(position),
                    isLast = isLast(position),
                    onItemListener,
                )
            15 ->
                (holder as ActionHolder).bind(
                    transcript,
                    isLast = isLast(position),
                    isFirst = isFirst(position),
                    onItemListener,
                )
            0 -> {
                // left empty
            }
            else ->
                (holder as UnknownHolder).bind(
                    transcript,
                    isLast = isLast(position),
                    isFirst = isFirst(position),
                    onItemListener,
                )
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position) ?: return 0
        return when {
            item.isText() && item.quoteContent.isNullOrEmpty() -> 1
            item.isText() -> -1
            item.isImage() && item.quoteContent.isNullOrEmpty() -> 2
            item.isImage() -> -2
            (item.isVideo() || item.isLive()) && item.quoteContent.isNullOrEmpty() -> 3
            (item.isVideo() || item.isLive()) -> -3
            item.isData() && item.quoteContent.isNullOrEmpty() -> 4
            item.isData() -> -4
            item.isAudio() && item.quoteContent.isNullOrEmpty() -> 5
            item.isAudio() -> -5
            item.isContact() && item.quoteContent.isNullOrEmpty() -> 6
            item.isContact() -> -6
            item.isSticker() -> 7
            item.isLocation() -> 9
            item.isAppCard() -> {
                if (item.appCardData?.newVersion == true) {
                    11
                }else{
                    10
                }
            }
            item.isPost() -> 12
            item.isTranscript() -> 13
            item.isRecall() -> 14
            item.isAppButtonGroup() -> 15
            else -> -99
        }
    }

    override fun onViewAttachedToWindow(holder: BaseViewHolder) {
        getItem(holder.layoutPosition)?.let {
            holder.listen(holder.itemView, it.messageId)
        }
    }

    override fun onViewDetachedFromWindow(holder: BaseViewHolder) {
        holder.stopListen()
    }

    override fun onCreateAttach(parent: ViewGroup): View =
        LayoutInflater.from(parent.context).inflate(R.layout.item_chat_unread, parent, false)

    override fun hasAttachView(position: Int): Boolean = false

    override fun onBindAttachView(view: View) {}

    private fun previous(position: Int): ChatHistoryMessageItem? {
        return if (position > 0) {
            getItem(position - 1)
        } else {
            null
        }
    }

    private fun next(position: Int): ChatHistoryMessageItem? {
        return if (position < itemCount - 1) {
            getItem(position + 1)
        } else {
            null
        }
    }

    private fun isFirst(position: Int): Boolean {
        val currentItem = getItem(position) ?: return false
        val previousItem = previous(position)
        return when {
            previousItem == null ->
                true
            previousItem.userId != currentItem.userId ->
                true
            !isSameDay(previousItem.createdAt, currentItem.createdAt) ->
                true
            else -> false
        }
    }

    override fun isLast(position: Int): Boolean {
        val currentItem = getItem(position) ?: return false
        val nextItem = next(position)
        return when {
            nextItem == null ->
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

    override fun isListLast(position: Int): Boolean {
        return position == itemCount - 1
    }

    override fun getHeaderId(position: Int) =
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

    open class OnItemListener {
        open fun onImageClick(
            messageItem: ChatHistoryMessageItem,
            view: View,
        ) {}

        open fun onFileClick(messageItem: ChatHistoryMessageItem) {}

        open fun onAudioFileClick(messageItem: ChatHistoryMessageItem) {}

        open fun onCancel(
            transcriptId: String?,
            messageId: String,
        ) {}

        open fun onRetryUpload(
            transcriptId: String?,
            messageId: String,
        ) {}

        open fun onRetryDownload(
            transcriptId: String?,
            messageId: String,
        ) {}

        open fun onUserClick(userId: String?) {}

        open fun onMentionClick(identityNumber: String) {}

        open fun onPhoneClick(phoneNumber: String) {}

        open fun onEmailClick(email: String) {}

        open fun onUrlClick(url: String) {}

        open fun onUrlLongClick(url: String) {}

        open fun onActionClick(
            action: String,
            userId: String?,
        ) {}

        open fun onAppCardClick(
            appCard: AppCardData,
            userId: String?,
        ) {}

        open fun onAudioClick(messageItem: ChatHistoryMessageItem) {}

        open fun onContactCardClick(userId: String) {}

        open fun onQuoteMessageClick(
            messageId: String,
            quoteMessageId: String?,
        ) {}

        open fun onPostClick(
            view: View,
            messageItem: ChatHistoryMessageItem,
        ) {}

        open fun onLocationClick(messageItem: ChatHistoryMessageItem) {}

        open fun onTextDoubleClick(messageItem: ChatHistoryMessageItem) {}

        open fun onTranscriptClick(messageItem: ChatHistoryMessageItem) {}

        open fun onMessageJump(messageId: String) {}

        open fun onMenu(
            view: View,
            messageItem: ChatHistoryMessageItem,
        ) {}
    }
}
