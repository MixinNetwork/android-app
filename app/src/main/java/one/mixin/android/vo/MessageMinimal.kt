package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity
data class MessageMinimal(
    @ColumnInfo(name = "rowid")
    val rowId: String,
    @ColumnInfo(name = "message_id")
    val messageId: String,
)
