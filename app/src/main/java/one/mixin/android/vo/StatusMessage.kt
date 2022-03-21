package one.mixin.android.vo

import androidx.room.ColumnInfo

class StatusMessage(
    @ColumnInfo(name = "message_id")
    val messageId: String,
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "expire_at")
    val expireAt: Long?
)
