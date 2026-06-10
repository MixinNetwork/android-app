package one.mixin.android.vo

import androidx.room.ColumnInfo

class FtsSearchResult(
    @ColumnInfo(name = "message_id")
    val messageId: String,
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "count")
    var messageCount: Int,
)
