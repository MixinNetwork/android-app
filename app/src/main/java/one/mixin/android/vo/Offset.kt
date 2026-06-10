package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offsets")
data class Offset(
    @PrimaryKey
    @ColumnInfo(name = "key")
    val key: String,
    @ColumnInfo(name = "timestamp")
    val timestamp: String,
)

const val STATUS_OFFSET = "messages_status_offset"
