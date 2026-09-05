package one.mixin.android.vo

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "offsets")
data class Offset(
    @PrimaryKey
    @ColumnInfo(name = "key")
    val key: String,
    @ColumnInfo(name = "timestamp")
    val timestamp: String,
)

const val STATUS_OFFSET = "messages_status_offset"
