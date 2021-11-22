package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Entity(tableName = "flood_messages")
@JsonClass(generateAdapter = true)
data class FloodMessage(
    @PrimaryKey
    @Json(name = "message_id")
    @ColumnInfo(name = "message_id")
    val messageId: String,
    @Json(name = "data")
    @ColumnInfo(name = "data")
    val data: String,
    @Json(name = "created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: String
)
