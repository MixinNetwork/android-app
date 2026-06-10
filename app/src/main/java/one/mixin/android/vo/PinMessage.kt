package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity(
    tableName = "pin_messages",
    indices = [
        Index(value = arrayOf("conversation_id", "created_at")),
    ],
)
@Serializable
data class PinMessage(
    @PrimaryKey
    @ColumnInfo(name = "message_id")
    @SerializedName("message_id")
    @SerialName("message_id")
    val messageId: String,
    @ColumnInfo(name = "conversation_id")
    @SerializedName("conversation_id")
    @SerialName("conversation_id")
    val conversationId: String,
    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    @SerialName("created_at")
    val createdAt: String,
)
