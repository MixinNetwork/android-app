package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Entity(tableName = "circle_conversations",
    primaryKeys = ["conversation_id", "circle_id"])
data class CircleConversation(
    @ColumnInfo(name = "conversation_id")
    @SerializedName("conversation_id")
    val conversationId: String,
    @ColumnInfo(name = "contact_id")
    @SerializedName("contact_id")
    val contactId: String?,
    @ColumnInfo(name = "circle_id")
    @SerializedName("circle_id")
    val circleId: String,
    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    val createdAt: String,
    @Expose
    @ColumnInfo(name = "pin_time")
    val pinTime: String?
)
