package one.mixin.android.vo

import androidx.room.ColumnInfo

class GroupInfo(
    @ColumnInfo(name = "name")
    val name: String?,
    @ColumnInfo(name = "count")
    val count: Int,
    @ColumnInfo(name = "icon_url")
    val iconUrl: String?,
    @ColumnInfo(name = "is_exist")
    val isExist: Boolean
)
