package one.mixin.android.ui.transfer.vo

import com.google.gson.annotations.SerializedName

class TransferMessageMention(
    @SerializedName("message_id")
    var messageId: String,
    @SerializedName("conversation_id")
    val conversationId: String,
    @SerializedName("mentions")
    val mentions: String?,
    @SerializedName("has_read")
    val hasRead: Boolean,
)
