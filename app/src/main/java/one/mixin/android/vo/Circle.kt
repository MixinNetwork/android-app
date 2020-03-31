package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "circles")
data class Circle(
    @PrimaryKey
    @ColumnInfo(name = "circle_id")
    val circleId: String,
    @ColumnInfo(name = "name")
    val name: String
)
