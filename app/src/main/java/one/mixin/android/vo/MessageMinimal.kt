package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity
data class MessageMinimal(
    @ColumnInfo(name = "rowid")
    val rowId: String,
    val id: String,
    @ColumnInfo(name = "created_at")
    val createdAt: String
)
