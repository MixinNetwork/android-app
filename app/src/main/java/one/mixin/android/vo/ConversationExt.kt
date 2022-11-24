package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversation_ext")
class ConversationExt(
    @PrimaryKey
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    @ColumnInfo(name = "count", defaultValue = "0")
    val count: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: String
)
