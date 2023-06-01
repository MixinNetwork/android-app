package one.mixin.android.db.pending

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import one.mixin.android.db.converter.MapTypeConverter

@Entity(
    tableName = "notification_ext",
)
class NotificationExt(
    @PrimaryKey
    @SerializedName("message_id")
    @ColumnInfo(name = "message_id")
    val messageId: String,
    @ColumnInfo(name = "user_map")
    @TypeConverters(MapTypeConverter::class)
    val userMap: Map<String, String>?,
    @ColumnInfo(name = "force")
    val force: Boolean,
    @ColumnInfo(name = "silent")
    val silent: Boolean,
)
