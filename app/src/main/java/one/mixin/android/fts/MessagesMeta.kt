package one.mixin.android.fts

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "messages_metas",
    indices = [
        Index(value = arrayOf("doc_id", "created_at"), orders = [Index.Order.DESC, Index.Order.DESC]),
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
