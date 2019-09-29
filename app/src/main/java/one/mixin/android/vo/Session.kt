package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.google.gson.annotations.SerializedName

@Entity(tableName = "sessions", primaryKeys = ["session_id", "user_id"])
data class Session(
    @SerializedName("session_id")
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    @SerializedName("user_id")
    @ColumnInfo(name = "user_id")
    val userId: String,
    @SerializedName("platform")
    @ColumnInfo(name = "platform")
    val platform: String?
)
