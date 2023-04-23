package one.mixin.android.ui.transfer.vo

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class TransferMessageMention(
    @SerializedName("message_id")
    @SerialName("message_id")
    var messageId: String,
    @SerializedName("conversation_id")
    @SerialName("conversation_id")
    val conversationId: String,
    @SerialName("mentions")
    @SerializedName("mentions")
    val mentions: String?,
    @SerialName("has_read")
    @SerializedName("has_read")
    val hasRead: Boolean,
)
