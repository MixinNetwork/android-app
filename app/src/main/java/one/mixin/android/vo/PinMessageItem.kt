package one.mixin.android.vo

import androidx.room.ColumnInfo

class PinMessageItem(
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "content")
    val content: String?,
    @ColumnInfo(name = "full_name")
    val userFullName: String?,
    @ColumnInfo(name = "mentions")
    val mentions: String?
)
