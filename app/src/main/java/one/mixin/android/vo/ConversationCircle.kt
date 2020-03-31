package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "conversation_circles",
    primaryKeys = ["conversation_id", "circle_id"])
data class ConversationCircle(
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    @ColumnInfo(name = "circle_id")
    val circleId: String,
    @ColumnInfo(name = "created_at")
    val createdAt: String
)
