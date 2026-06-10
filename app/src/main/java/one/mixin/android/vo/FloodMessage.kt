package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "flood_messages")
data class FloodMessage(
    @PrimaryKey
    @SerializedName("message_id")
    @ColumnInfo(name = "message_id")
    val messageId: String,
    @SerializedName("data")
    @ColumnInfo(name = "data")
    val data: String,
    @SerializedName("created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: String,
)
