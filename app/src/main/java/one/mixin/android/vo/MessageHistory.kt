package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages_history")
class MessageHistory(
    @PrimaryKey
    @ColumnInfo(name = "message_id")
    val messageId: String
    // @ColumnInfo(name = "created_at")
    // val createdAt: String,
)
