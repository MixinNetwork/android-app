package one.mixin.android.fts

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages_metas",
    indices = [
        Index(value = arrayOf("doc_id", "created_at")),
        Index(value = arrayOf("conversation_id", "user_id", "category")),
    ],
)
class MessagesMeta(
    @ColumnInfo(name = "doc_id")
    var docId: Long,
    @PrimaryKey
    @ColumnInfo(name = "message_id")
    var messageId: String,
    @ColumnInfo(name = "conversation_id")
    var conversationId: String,
    @ColumnInfo(name = "category")
    var category: String,
    @ColumnInfo(name = "user_id")
    var userId: String,
    @ColumnInfo(name = "created_at")
    var createdAt: Long,
)
