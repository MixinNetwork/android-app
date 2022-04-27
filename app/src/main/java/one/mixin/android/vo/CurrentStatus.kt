package one.mixin.android.vo

import androidx.room.ColumnInfo

class CurrentStatus(
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    @ColumnInfo(name = "status")
    val status: String,
)
