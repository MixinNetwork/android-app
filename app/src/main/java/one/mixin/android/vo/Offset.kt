package one.mixin.android.vo

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity(tableName = "offsets")
data class Offset(
    @PrimaryKey
    @ColumnInfo(name = "key")
    val key: String,
    @ColumnInfo(name = "timestamp")
    val timestamp: String
)

const val STATUS_OFFSET = "messages_status_offset"
