package one.mixin.android.crypto.vo

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "ratchet_sender_keys", primaryKeys = ["group_id", "sender_id"])
class RatchetSenderKey(
    @ColumnInfo(name = "group_id")
    val groupId: String,
    @ColumnInfo(name = "sender_id")
    val senderId: String,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "message_id")
    val messageId: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
)

enum class RatchetStatus { REQUESTING }
