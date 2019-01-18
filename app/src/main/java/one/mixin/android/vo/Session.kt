package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "sessions", indices = [(Index(value = arrayOf("user_id")))])
data class Session(
    @PrimaryKey
    @ColumnInfo(name = "session_id")
    @SerializedName("session_id")
    val sessionId: String,
    @ColumnInfo(name = "user_id")
    @SerializedName("user_id")
    val userId: String,
    @ColumnInfo(name = "device_id")
    @SerializedName("device_id")
    val deviceId: Int
)
