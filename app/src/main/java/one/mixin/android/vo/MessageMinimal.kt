package one.mixin.android.vo

import androidx.room3.ColumnInfo
import androidx.room3.Entity

@Entity
data class MessageMinimal(
    @ColumnInfo(name = "rowid")
    val rowId: String,
    val id: String,
)
