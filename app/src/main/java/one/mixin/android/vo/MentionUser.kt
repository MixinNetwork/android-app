package one.mixin.android.vo

import androidx.room.ColumnInfo

data class MentionUser(
    @ColumnInfo(name = "identity_number")
    val identityNumber: String,
    @ColumnInfo(name = "full_name")
    val fullName: String
)
