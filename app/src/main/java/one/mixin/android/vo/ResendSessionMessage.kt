package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "resend_session_messages", primaryKeys = ["message_id", "user_id", "session_id"])
class ResendSessionMessage(
    @ColumnInfo(name = "message_id")
    val messageId: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    @ColumnInfo(name = "status")
    val status: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
)
