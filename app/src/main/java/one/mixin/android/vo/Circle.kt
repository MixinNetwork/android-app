package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Entity(tableName = "circles")
@JsonClass(generateAdapter = true)
data class Circle(
    @PrimaryKey
    @Json(name = "circle_id")
    @ColumnInfo(name = "circle_id")
    val circleId: String,
    @Json(name = "name")
    @ColumnInfo(name = "name")
    val name: String,
    @Json(name = "created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    // todo @Expose
    @ColumnInfo(name = "ordered_at")
    val orderedAt: String?
)

data class CircleOrder(
    @ColumnInfo(name = "circle_id")
    val circleId: String,
    @ColumnInfo(name = "ordered_at")
    val orderedAt: String
)

data class CircleName(val name: String)
