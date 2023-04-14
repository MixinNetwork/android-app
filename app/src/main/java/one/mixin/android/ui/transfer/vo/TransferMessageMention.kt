package one.mixin.android.ui.transfer.vo

import androidx.room.ColumnInfo
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class TransferMessageMention(
    @SerializedName("message_id")
    @ColumnInfo(name = "message_id")
    @SerialName("message_id")
    var messageId: String,
    @SerializedName("conversation_id")
    @ColumnInfo(name = "conversation_id")
    @SerialName("conversation_id")
    val conversationId: String,
    @SerializedName("has_read")
    @ColumnInfo(name = "has_read")
    @SerialName("has_read")
    val hasRead: Boolean,
)