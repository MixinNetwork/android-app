package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "mention_message",
    foreignKeys = [(ForeignKey(
        entity = Message::class,
        onDelete = ForeignKey.CASCADE,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("message_id")
    ))]
)
class MentionMessage(
    @PrimaryKey
    @ColumnInfo(name = "message_id")
    var messageId: String,
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    @ColumnInfo(name = "created_at")
    val createdAt: String
)