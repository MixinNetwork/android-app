package one.mixin.android.ui.conversation.transcript

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatAudioBinding
import one.mixin.android.databinding.ItemChatAudioQuoteBinding
import one.mixin.android.databinding.ItemChatContactCardBinding
import one.mixin.android.databinding.ItemChatContactCardQuoteBinding
import one.mixin.android.databinding.ItemChatFileBinding
import one.mixin.android.databinding.ItemChatFileQuoteBinding
import one.mixin.android.databinding.ItemChatImageBinding
import one.mixin.android.databinding.ItemChatImageQuoteBinding
import one.mixin.android.databinding.ItemChatLocationBinding
import one.mixin.android.databinding.ItemChatStickerBinding
import one.mixin.android.databinding.ItemChatTextBinding
import one.mixin.android.databinding.ItemChatTextQuoteBinding
import one.mixin.android.databinding.ItemChatTimeBinding
import one.mixin.android.databinding.ItemChatTranscriptBinding
import one.mixin.android.databinding.ItemChatVideoBinding
import one.mixin.android.databinding.ItemChatVideoQuoteBinding
import one.mixin.android.extension.hashForDate
import one.mixin.android.extension.isSameDay
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.ui.conversation.holder.BaseViewHolder
import one.mixin.android.ui.conversation.holder.TimeHolder
import one.mixin.android.ui.conversation.transcript.holder.AudioHolder
import one.mixin.android.ui.conversation.transcript.holder.AudioQuoteHolder
import one.mixin.android.ui.conversation.transcript.holder.ContactCardHolder
import one.mixin.android.ui.conversation.transcript.holder.ContactCardQuoteHolder
import one.mixin.android.ui.conversation.transcript.holder.FileHolder
import one.mixin.android.ui.conversation.transcript.holder.FileQuoteHolder
import one.mixin.android.ui.conversation.transcript.holder.ImageHolder
import one.mixin.android.ui.conversation.transcript.holder.ImageQuoteHolder
import one.mixin.android.ui.conversation.transcript.holder.LocationHolder
import one.mixin.android.ui.conversation.transcript.holder.StickerHolder
import one.mixin.android.ui.conversation.transcript.holder.TextHolder
import one.mixin.android.ui.conversation.transcript.holder.TextQuoteHolder
import one.mixin.android.ui.conversation.transcript.holder.TranscriptHolder
import one.mixin.android.ui.conversation.transcript.holder.VideoHolder
import one.mixin.android.ui.conversation.transcript.holder.VideoQuoteHolder
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.isAudio
import one.mixin.android.vo.isContact
import one.mixin.android.vo.isData
import one.mixin.android.vo.isImage
import one.mixin.android.vo.isLocation
import one.mixin.android.vo.isSticker
import one.mixin.android.vo.isText
import one.mixin.android.vo.isTranscript
import one.mixin.android.vo.isVideo
import one.mixin.android.widget.MixinStickyRecyclerHeadersAdapter
import kotlin.math.abs

class TranscriptAdapter(
    private val attachmentTranscripts: List<MessageItem>,
    private val onItemListener: ConversationAdapter.OnItemListener,
    private val onMessageItemClickListener: (View, MessageItem) -> Unit
) :
    RecyclerView.Adapter<BaseViewHolder>(), MixinStickyRecyclerHeadersAdapter<TimeHolder> {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return when (viewType) {
            1 -> TextHolder(ItemChatTextBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            -1 -> TextQuoteHolder(ItemChatTextQuoteBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            2 -> ImageHolder(ItemChatImageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            -2 -> ImageQuoteHolder(ItemChatImageQuoteBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            3 -> VideoHolder(ItemChatVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            -3 -> VideoQuoteHolder(ItemChatVideoQuoteBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            4 -> FileHolder(ItemChatFileBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            -4 -> FileQuoteHolder(ItemChatFileQuoteBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            5 -> AudioHolder(ItemChatAudioBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            -5 -> AudioQuoteHolder(ItemChatAudioQuoteBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            6 -> ContactCardHolder(ItemChatContactCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            -6 -> ContactCardQuoteHolder(ItemChatContactCardQuoteBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            7 -> StickerHolder(ItemChatStickerBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            8 -> TranscriptHolder(ItemChatTranscriptBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            9 -> LocationHolder(ItemChatLocationBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            else -> TextHolder(ItemChatTextBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        return when (getItemViewType(position)) {
            1 -> (holder as TextHolder).bind(attachmentTranscripts[position], isLast = isLast(position), isFirst = isFirst(position), onItemListener)
            -1 -> (holder as TextQuoteHolder).bind(attachmentTranscripts[position], isLast = isLast(position), isFirst = isFirst(position), onItemListener)
            2 -> (holder as ImageHolder).bind(attachmentTranscripts[position], isLast = isLast(position), isFirst = isFirst(position)) {
                onMessageItemClickListener(it, attachmentTranscripts[position])
            }
            -2 -> (holder as ImageQuoteHolder).bind(attachmentTranscripts[position], isLast = isLast(position), isFirst = isFirst(position)) {
                onMessageItemClickListener(it, attachmentTranscripts[position])
            }
            3 -> (holder as VideoHolder).bind(attachmentTranscripts[position], isLast = isLast(position), isFirst = isFirst(position)) {
                onMessageItemClickListener(it, attachmentTranscripts[position])
            }
            -3 -> (holder as VideoQuoteHolder).bind(attachmentTranscripts[position], isLast = isLast(position), isFirst = isFirst(position)) {
                onMessageItemClickListener(it, attachmentTranscripts[position])
            }
            4 -> (holder as FileHolder).bind(attachmentTranscripts[position], isLast = isLast(position), isFirst = isFirst(position))
            -4 -> (holder as FileQuoteHolder).bind(attachmentTranscripts[position], isLast = isLast(position), isFirst = isFirst(position))
            5 -> (holder as AudioHolder).bind(attachmentTranscripts[position], isLast = isLast(position), isFirst = isFirst(position)) {
                onMessageItemClickListener(it, attachmentTranscripts[position])
            }
            -5 -> (holder as AudioQuoteHolder).bind(attachmentTranscripts[position], isLast = isLast(position), isFirst = isFirst(position)) {
                onMessageItemClickListener(it, attachmentTranscripts[position])
            }
            6 -> (holder as ContactCardHolder).bind(attachmentTranscripts[position], isLast = isLast(position), isFirst = isFirst(position))
            -6 -> (holder as ContactCardQuoteHolder).bind(attachmentTranscripts[position], isLast = isLast(position), isFirst = isFirst(position))
            7 -> (holder as StickerHolder).bind(attachmentTranscripts[position], isFirst = isFirst(position))
            8 -> (holder as TranscriptHolder).bind(attachmentTranscripts[position], isLast = isLast(position), isFirst = isFirst(position))
            9 -> (holder as LocationHolder).bind(attachmentTranscripts[position], isLast = isLast(position), isFirst = isFirst(position))
            else -> (holder as TextHolder).bind(attachmentTranscripts[position], isLast = isLast(position), isFirst = isFirst(position), onItemListener)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return when {
            item.isText() && item.quoteContent.isNullOrEmpty() -> 1
            item.isText() -> -1
            item.isImage() && item.quoteContent.isNullOrEmpty() -> 2
            item.isImage() -> -2
            item.isVideo() && item.quoteContent.isNullOrEmpty() -> 3
            item.isVideo() -> -3
            item.isData() && item.quoteContent.isNullOrEmpty() -> 4
            item.isData() -> -4
            item.isAudio() && item.quoteContent.isNullOrEmpty() -> 5
            item.isAudio() -> -5
            item.isContact() && item.quoteContent.isNullOrEmpty() -> 6
            item.isContact() -> -6
            item.isSticker() -> 7
            item.isTranscript() -> 8
            item.isLocation() -> 9
            else -> -99
        }
    }

    private fun getItem(position: Int): MessageItem {
        return attachmentTranscripts[position]
    }

    override fun getItemCount(): Int = attachmentTranscripts.size
    override fun onCreateAttach(parent: ViewGroup): View = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_unread, parent, false)

    override fun hasAttachView(position: Int): Boolean = false

    override fun onBindAttachView(view: View) {}

    private fun previous(position: Int): MessageItem? {
        return if (position > 0) {
            getItem(position - 1)
        } else {
            null
        }
    }

    private fun next(position: Int): MessageItem? {
        return if (position < itemCount - 1) {
            getItem(position + 1)
        } else {
            null
        }
    }

    private fun isFirst(position: Int): Boolean {
        val currentItem = getItem(position)
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
        val currentItem = getItem(position)
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
        return position == 0
    }

    override fun getHeaderId(position: Int) = getItem(position).notNullWithElse(
        {
            abs(it.createdAt.hashForDate())
        },
        0
    )

    override fun onCreateHeaderViewHolder(parent: ViewGroup): TimeHolder =
        TimeHolder(ItemChatTimeBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindHeaderViewHolder(holder: TimeHolder, position: Int) {
        getItem(position).let {
            holder.bind(it.createdAt)
        }
    }
}
