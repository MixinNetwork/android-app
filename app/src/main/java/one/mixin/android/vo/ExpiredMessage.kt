package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(
    tableName = "expired_messages",
)
class ExpiredMessage(
    @PrimaryKey
    @ColumnInfo(name = "message_id")
    @SerializedName("message_id")
    val messageId: String,
    @ColumnInfo(name = "expire_in")
    @SerializedName("expire_in")
    val expireIn: Long,
    @ColumnInfo(name = "expire_at")
    @SerializedName("expire_at")
    val expireAt: Long?,
)
