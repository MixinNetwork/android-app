package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Entity(
    tableName = "circle_conversations",
    primaryKeys = ["conversation_id", "circle_id"]
)
@JsonClass(generateAdapter = true)
data class CircleConversation(
    @ColumnInfo(name = "conversation_id")
    @Json(name ="conversation_id")
    val conversationId: String,
    @ColumnInfo(name = "circle_id")
    @Json(name ="circle_id")
    val circleId: String,
    @ColumnInfo(name = "user_id")
    @Json(name ="user_id")
    val userId: String?,
    @ColumnInfo(name = "created_at")
    @Json(name ="created_at")
    val createdAt: String,
    // todo @Expose
    @ColumnInfo(name = "pin_time")
    val pinTime: String?
)

enum class CircleConversationAction { ADD, REMOVE }
