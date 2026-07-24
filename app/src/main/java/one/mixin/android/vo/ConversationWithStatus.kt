package one.mixin.android.vo

import androidx.room3.ColumnInfo

class ConversationWithStatus(
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "status")
    val status: MessageStatus,
)
