package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity(
    tableName = "expired_messages",
)
@Serializable
class ExpiredMessage(
    @PrimaryKey
    @ColumnInfo(name = "message_id")
    @SerializedName("message_id")
    @SerialName("message_id")
    val messageId: String,
    @ColumnInfo(name = "expire_in")
    @SerializedName("expire_in")
    @SerialName("expire_in")
    val expireIn: Long,
    @ColumnInfo(name = "expire_at")
    @SerializedName("expire_at")
    @SerialName("expire_at")
    val expireAt: Long?,
)
