package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "resend_messages", primaryKeys = ["message_id", "user_id"])
class ResendMessage(
    @ColumnInfo(name = "message_id")
    val messageId: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "status")
    val status: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
)
