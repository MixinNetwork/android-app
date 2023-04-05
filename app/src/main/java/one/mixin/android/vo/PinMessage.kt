package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(
    tableName = "pin_messages",
    indices = [
        Index(value = arrayOf("conversation_id")),
    ],
)
data class PinMessage(
    @PrimaryKey
    @ColumnInfo(name = "message_id")
    @SerializedName("message_id")
    val messageId: String,
    @ColumnInfo(name = "conversation_id")
    @SerializedName("conversation_id")
    val conversationId: String,
    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    val createdAt: String,
)
