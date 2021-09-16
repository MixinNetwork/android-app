package one.mixin.android.ui.conversation.chathistory.holder

import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.TextViewCompat
import com.google.gson.Gson
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatTranscriptBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.maxItemWidth
import one.mixin.android.extension.round
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.session.Session
import one.mixin.android.ui.conversation.chathistory.TranscriptAdapter
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.AppCardData
import one.mixin.android.vo.ChatHistoryMessageItem
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.TranscriptMinimal
import one.mixin.android.vo.isAudio
import one.mixin.android.vo.isContact
import one.mixin.android.vo.isData
import one.mixin.android.vo.isImage
import one.mixin.android.vo.isLive
import one.mixin.android.vo.isLocation
import one.mixin.android.vo.isPost
import one.mixin.android.vo.isSticker
import one.mixin.android.vo.isTranscript
import one.mixin.android.vo.isVideo
import org.jetbrains.anko.dip
import org.jetbrains.anko.textColorResource

class TranscriptHolder constructor(val binding: ItemChatTranscriptBinding) :
    BaseViewHolder(binding.root) {
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
        messageItem: ChatHistoryMessageItem,
        isLast: Boolean,
        isFirst: Boolean = false,
        onItemListener: TranscriptAdapter.OnItemListener
    ) {
        super.bind(messageItem)
        val nightMode = itemView.context.isNightMode()
        val isMe = messageItem.userId == Session.getAccountId()
        if (nightMode) {
            if (isMe) {
                binding.chatTv.textColorResource = R.color.textTranscriptNight
            } else {
                binding.chatTv.textColorResource = R.color.textTranscriptNightOther
            }
        } else {
            binding.chatTv.textColorResource = R.color.textTranscript
        }
        if (binding.chatTv.tag != messageItem.messageId) {
            if (!messageItem.content.isNullOrEmpty()) {
                val transcripts = GsonHelper.customGson.fromJson(
                    messageItem.content,
                    Array<TranscriptMinimal>::class.java
                )
                val str = StringBuilder()
                transcripts.forEach {
                    when {
                        it.isImage() -> {
                            str.append("${it.name}: [${itemView.context.getString(R.string.photo)}]\n")
                        }
                        it.isVideo() -> {
                            str.append("${it.name}: [${itemView.context.getString(R.string.video)}]\n")
                        }
                        it.isData() -> {
                            str.append("${it.name}: [${itemView.context.getString(R.string.document)}]\n")
                        }
                        it.isAudio() -> {
                            str.append("${it.name}: [${itemView.context.getString(R.string.audio)}]\n")
                        }
                        it.isPost() -> {
                            str.append("${it.name}: [${itemView.context.getString(R.string.post)}]\n")
                        }
                        it.isLocation() -> {
                            str.append("${it.name}: [${itemView.context.getString(R.string.location)}]\n")
                        }
                        it.isTranscript() -> {
                            str.append("${it.name}: [${itemView.context.getString(R.string.transcript)}]\n")
                        }
                        it.isContact() -> {
                            str.append("${it.name}: [${itemView.context.getString(R.string.contact)}]\n")
                        }
                        it.isLive() -> {
                            str.append("${it.name}: [${itemView.context.getString(R.string.live)}]\n")
                        }
                        it.isSticker() -> {
                            str.append("${it.name}: [${itemView.context.getString(R.string.sticker)}]\n")
                        }
                        it.type == MessageCategory.APP_CARD.name -> {
                            try {
                                val cardData = Gson().fromJson(it.content, AppCardData::class.java)
                                if (cardData.title.isBlank()) {
                                    str.append("${it.name}: [${itemView.context.getString(R.string.card)}]\n")
                                } else {
                                    str.append("${it.name}: [${cardData.title}]\n")
                                }
                            } catch (e: Exception) {
                                str.append("${it.name}: [${itemView.context.getString(R.string.card)}]\n")
                            }
                        }
                        else -> str.append("${it.name}: ${it.content}\n")
                    }
                }
                binding.chatTv.text = str.removeSuffix("\n")
                binding.chatTv.tag = messageItem.messageId
            } else {
                binding.chatTv.text = null
            }
        }
        binding.chatLayout.setOnClickListener {
            onItemListener.onTranscriptClick(messageItem)
        }
        if (isFirst && !isMe) {
            binding.chatName.visibility = View.VISIBLE
            binding.chatName.text = messageItem.userFullName
            if (messageItem.appId != null) {
                binding.chatName.setCompoundDrawables(null, null, botIcon, null)
                binding.chatName.compoundDrawablePadding = itemView.dip(3)
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
            binding.chatName.compoundDrawablePadding = itemView.dip(3)
        } else {
            binding.chatName.setCompoundDrawables(null, null, null, null)
        }
        binding.chatTime.timeAgoClock(messageItem.createdAt)
        setStatusIcon(
            isMe,
            MessageStatus.DELIVERED.name,
            isSecret = false,
            isRepresentative = false,
            isWhite = true
        ) { statusIcon, secretIcon, representativeIcon ->
            statusIcon?.setBounds(0, 0, 12.dp, 12.dp)
            secretIcon?.setBounds(0, 0, dp8, dp8)
            representativeIcon?.setBounds(0, 0, dp8, dp8)
            TextViewCompat.setCompoundDrawablesRelative(
                binding.chatTime,
                secretIcon ?: representativeIcon,
                null,
                statusIcon,
                null
            )
        }
        chatLayout(isMe, isLast)
        if (messageItem.transcriptId == null) {
            binding.root.setOnClickListener {
                onItemListener.onMenu(binding.chatJump, messageItem)
            }
            chatJumpLayout(binding.chatJump, isMe, messageItem.messageId, R.id.chat_layout, onItemListener)
        }
    }
}
