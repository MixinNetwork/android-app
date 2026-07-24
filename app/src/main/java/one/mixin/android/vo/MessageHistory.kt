package one.mixin.android.vo

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "messages_history")
class MessageHistory(
    @PrimaryKey
    @ColumnInfo(name = "message_id")
    val messageId: String,
    // @ColumnInfo(name = "created_at")
    // val createdAt: String,
)
