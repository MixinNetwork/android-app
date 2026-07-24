package one.mixin.android.vo

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "conversation_ext")
class ConversationExt(
    @PrimaryKey
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    @ColumnInfo(name = "count", defaultValue = "0")
    val count: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
)
