package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity(tableName = "recall_messages")
@Serializable
data class RecallMessage(
    @PrimaryKey
    @ColumnInfo(name = "message_id")
    @SerializedName("message_id")
    @SerialName("message_id")
    val messageId: String,
    @ColumnInfo(name = "user_id")
    @SerializedName("user_id")
    @SerialName("user_id")
    val userId: String,
)
