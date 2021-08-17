package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "pin_messages",
    foreignKeys = [
        (
            ForeignKey(
                entity = Conversation::class,
                onDelete = ForeignKey.CASCADE,
                parentColumns = arrayOf("conversation_id"),
                childColumns = arrayOf("conversation_id")
            )
            ),
        (
            ForeignKey(
                entity = Message::class,
                onDelete = ForeignKey.CASCADE,
                parentColumns = arrayOf("id"),
                childColumns = arrayOf("message_id")
            )
            )
    ]
)
data class PinMessage(
    @PrimaryKey
    @ColumnInfo(name = "message_id")
    val messageId: String,
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    @ColumnInfo(name = "created_at")
    val createdAt: String
)
