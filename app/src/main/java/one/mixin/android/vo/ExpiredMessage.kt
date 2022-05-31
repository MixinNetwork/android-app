package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "expired_messages"
)
class ExpiredMessage(
    @PrimaryKey
    @ColumnInfo(name = "message_id")
    val messageId: String,
    @ColumnInfo(name = "expire_in")
    val expireIn: Long,
    @ColumnInfo(name = "expire_at")
    val expireAt: Long?
)
