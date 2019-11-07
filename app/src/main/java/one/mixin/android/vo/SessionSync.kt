package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "session_sync")
data class SessionSync(
    @PrimaryKey
    @SerializedName("conversation_id")
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    @SerializedName("created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: String?
)
