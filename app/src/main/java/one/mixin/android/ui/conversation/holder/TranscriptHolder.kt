package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.gson.Gson
import one.mixin.android.Constants.Colors.SELECT_COLOR
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatTranscriptBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.maxItemWidth
import one.mixin.android.extension.round
import one.mixin.android.extension.textColorResource
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.ui.conversation.holder.base.BaseViewHolder
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.AppCardData
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.TranscriptMinimal
import one.mixin.android.vo.isAudio
import one.mixin.android.vo.isContact
import one.mixin.android.vo.isData
import one.mixin.android.vo.isImage
import one.mixin.android.vo.isLive
import one.mixin.android.vo.isLocation
import one.mixin.android.vo.isPost
import one.mixin.android.vo.isSecret
import one.mixin.android.vo.isSticker
import one.mixin.android.vo.isTranscript
import one.mixin.android.vo.isVideo

class TranscriptHolder constructor(val binding: ItemChatTranscriptBinding) : BaseViewHolder(binding.root) {
    init {
        binding.chatTv.layoutParams.width = itemView.context.maxItemWidth()
        binding.chatTv.round(3.dp)
    }

    override fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean) {
        super.chatLayout(isMe, isLast, isBlink)
        if (isMe) {
            (binding.chatLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 1f
            (binding.chatTime.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 12.dp
            (binding.chatTitle.layoutParams as ViewGroup.MarginLayoutParams).marginStart = 8.dp
            (binding.chatPost.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 10.dp
            (binding.chatTv.layoutParams as ViewGroup.MarginLayoutParams).apply {
                marginStart = 4.dp
                marginEnd = 10.dp
            }
        } else {
            (binding.chatLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 0f
            (binding.chatTime.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 6.dp
            (binding.chatTitle.layoutParams as ViewGroup.MarginLayoutParams).marginStart = 14.dp
            (binding.chatPost.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 2.dp
            (binding.chatTv.layoutParams as ViewGroup.MarginLayoutParams).apply {
                marginStart = 10.dp
                marginEnd = 4.dp
            }
        }
        val lp = (binding.chatLayout.layoutParams as ConstraintLayout.LayoutParams)
        if (isMe) {
            lp.horizontalBias = 1f
            if (isLast) {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_transcript_me_last,
                    R.drawable.chat_bubble_transcript_me_last_night
                )
            } else {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_transcript_me,
                    R.drawable.chat_bubble_transcript_me_night
                )
            }
        } else {
            lp.horizontalBias = 0f
            if (isLast) {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_post_other_last,
                    R.drawable.chat_bubble_post_other_last_night
                )
            } else {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_post_other,
                    R.drawable.chat_bubble_post_other_night
                )
            }
        }
    }

    fun bind(
        messageItem: MessageItem,
        isLast: Boolean,
        isFirst: Boolean = false,
        hasSelect: Boolean,
        isSelect: Boolean,
        isRepresentative: Boolean,
        onItemListener: ConversationAdapter.OnItemListener,
    ) {
        super.bind(messageItem)
        if (hasSelect && isSelect) {
            itemView.setBackgroundColor(SELECT_COLOR)
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT)
        }
        val nightMode = itemView.context.isNightMode()
        if (nightMode) {
            if (isMe) {
                binding.chatTv.textColorResource = R.color.textTranscriptNight
            } else {
                binding.chatTv.textColorResource = R.color.textTranscriptNightOther
            }
        } else {
            binding.chatTv.textColorResource = R.color.textTranscript
        }
        binding.chatTv.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(messageItem, absoluteAdapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
                true
            }
        }

        binding.chatTv.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
            } else {
                onItemListener.onTranscriptClick(messageItem)
            }
        }
        binding.chatLayout.setOnClickListener {
            if (!hasSelect) {
                onItemListener.onTranscriptClick(messageItem)
            }
        }

        if (binding.chatTv.tag != messageItem.messageId) {
            if (!messageItem.content.isNullOrEmpty()) {
                val transcripts = GsonHelper.customGson.fromJson(messageItem.content, Array<TranscriptMinimal>::class.java)
                val str = StringBuilder()
                transcripts.forEach {
                    when {
                        it.isImage() -> {
                            str.append("${it.name}: [${itemView.context.getString(R.string.Photo)}]\n")
                        }
                        it.isVideo() -> {
                            str.append("${it. name}: [${itemView.context.getString(R.string.Video)}]\n")
                        }
                        it.isData() -> {
                            str.append("${it. name}: [${itemView.context.getString(R.string.Document)}]\n")
                        }
                        it.isAudio() -> {
                            str.append("${it. name}: [${itemView.context.getString(R.string.Audio)}]\n")
                        }
                        it.isPost() -> {
                            str.append("${it. name}: [${itemView.context.getString(R.string.Post)}]\n")
                        }
                        it.isLocation() -> {
                            str.append("${it. name}: [${itemView.context.getString(R.string.Location)}]\n")
                        }
                        it.isTranscript() -> {
                            str.append("${it. name}: [${itemView.context.getString(R.string.Transcript)}]\n")
                        }
                        it.isContact() -> {
                            str.append("${it. name}: [${itemView.context.getString(R.string.Contact)}]\n")
                        }
                        it.isLive() -> {
                            str.append("${it. name}: [${itemView.context.getString(R.string.Live)}]\n")
                        }
                        it.isSticker() -> {
                            str.append("${it. name}: [${itemView.context.getString(R.string.Sticker)}]\n")
                        }
                        it.type == MessageCategory.APP_CARD.name -> {
                            try {
                                val cardData = Gson().fromJson(it.content, AppCardData::class.java)
                                if (cardData.title.isBlank()) {
                                    str.append("${it.name}: [${itemView.context.getString(R.string.Card)}]\n")
                                } else {
                                    str.append("${it.name}: [${cardData.title}]\n")
                                }
                            } catch (e: Exception) {
                                str.append("${it. name}: [${itemView.context.getString(R.string.Card)}]\n")
                            }
                        }
                        else -> str.append("${it. name}: ${it.content}\n")
                    }
                }
                binding.chatTv.text = str.removeSuffix("\n")
                binding.chatTv.tag = messageItem.messageId
            } else {
                binding.chatTv.text = null
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

        binding.chatTime.load(
            isMe,
            messageItem.createdAt,
            messageItem.status,
            messageItem.isPin ?: false,
            isRepresentative = isRepresentative,
            isSecret = messageItem.isSecret(),
            isWhite = true
        )
        chatLayout(isMe, isLast)
    }
}
