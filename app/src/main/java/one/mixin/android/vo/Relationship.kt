package one.mixin.android.vo

import androidx.room.ColumnInfo

class Relationship(
    @ColumnInfo(name = "relationship")
    val relationship: String,
)