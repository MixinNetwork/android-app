package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity
data class MessageMinimal(
    val id: String,
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "created_at")
    val createdAt: String
)
