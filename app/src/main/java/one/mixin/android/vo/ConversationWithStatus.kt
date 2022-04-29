package one.mixin.android.vo

import androidx.room.ColumnInfo

class ConversationWithStatus(
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    @ColumnInfo(name = "status")
    val status: MessageStatus,
)
