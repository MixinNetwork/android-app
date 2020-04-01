package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "circle_conversations",
    primaryKeys = ["conversation_id", "circle_id"])
data class CircleConversation(
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    @ColumnInfo(name = "circle_id")
    val circleId: String,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "pin_time")
    val pinTime: String?
)
