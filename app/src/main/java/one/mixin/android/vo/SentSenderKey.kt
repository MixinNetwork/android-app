package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import one.mixin.android.extension.nowInUtc

@Entity(tableName = "sent_sender_keys", primaryKeys = ["conversation_id", "user_id"])
class SentSenderKey(
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "sent_to_server")
    val sentToServer: Int,
    @ColumnInfo(name = "sender_key_id")
    val senderKeyId: Int? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: String? = nowInUtc(),
)
