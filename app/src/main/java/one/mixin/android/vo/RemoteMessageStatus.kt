package one.mixin.android.vo

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "remote_messages_status",
    indices = [Index(value = arrayOf("conversation_id", "status"))],
)
class RemoteMessageStatus(
    @PrimaryKey
    @ColumnInfo(name = "message_id")
    val messageId: String,
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    @ColumnInfo(name = "status")
    val status: String,
)
