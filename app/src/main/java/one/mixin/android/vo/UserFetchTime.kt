package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_fetch_times",
)
class UserFetchTime(
    @PrimaryKey
    @ColumnInfo("user_id")
    val userId: String,
    @ColumnInfo("last_fetch_at")
    val lastFetchAt: Long,
)