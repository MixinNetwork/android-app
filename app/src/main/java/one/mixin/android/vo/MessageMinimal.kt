package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity
data class MessageMinimal(
    @ColumnInfo(name = "message_id")
    val id: String,
    @ColumnInfo(name = "created_at")
    val createdAt: String
)
