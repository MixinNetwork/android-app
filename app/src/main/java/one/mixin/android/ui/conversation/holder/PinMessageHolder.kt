package one.mixin.android.ui.conversation.holder

import android.content.Context
import android.graphics.Color
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatSystemBinding
import one.mixin.android.extension.fromJson
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.*

class PinMessageHolder constructor(val binding: ItemChatSystemBinding) : BaseViewHolder(binding.root) {

    var context: Context = itemView.context
    private fun getText(id: Int) = context.getText(id).toString()

    fun bind(
        messageItem: MessageItem,
        hasSelect: Boolean,
        isSelect: Boolean,
        onItemListener: ConversationAdapter.OnItemListener
    ) {
        val id = meId
        if (hasSelect && isSelect) {
            itemView.setBackgroundColor(SELECT_COLOR)
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT)
        }
        val pinMessage = GsonHelper.customGson.fromJson(messageItem.content, PinMessageMinimal::class.java)
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
            } else {
                onItemListener.onQuoteMessageClick(messageItem.messageId, pinMessage.messageId)
            }
        }

        binding.chatInfo.text =
            String.format(
                getText(R.string.chat_pin_message),
                if (id == messageItem.userId) {
                    getText(R.string.chat_you_start)
                } else {
                    messageItem.participantFullName
                },
                when {
                    pinMessage.isImage() -> getText(R.string.chat_pin_image_message)
                    pinMessage.isVideo() -> getText(R.string.chat_pin_video_message)
                    pinMessage.isLive() -> getText(R.string.chat_pin_live_message)
                    pinMessage.isData() -> getText(R.string.chat_pin_data_message)
                    pinMessage.isAudio() -> getText(R.string.chat_pin_audio_message)
                    pinMessage.isSticker() -> getText(R.string.chat_pin_sticker_message)
                    pinMessage.isContact() -> getText(R.string.chat_pin_contact_message)
                    pinMessage.isPost() -> getText(R.string.chat_pin_post_message)
                    pinMessage.isLocation() -> getText(R.string.chat_pin_location_message)
                    pinMessage.isTranscript() -> getText(R.string.chat_pin_transcript_message)
                    else -> " \"${pinMessage.content}\""
                }
            )
    }
}
