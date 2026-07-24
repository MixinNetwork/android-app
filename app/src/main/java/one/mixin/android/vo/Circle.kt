package one.mixin.android.vo

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Entity(tableName = "circles")
data class Circle(
    @PrimaryKey
    @SerializedName("circle_id")
    @ColumnInfo(name = "circle_id")
    val circleId: String,
    @SerializedName("name")
    @ColumnInfo(name = "name")
    val name: String,
    @SerializedName("created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @Expose
    @ColumnInfo(name = "ordered_at")
    val orderedAt: String?,
)

data class CircleOrder(
    @ColumnInfo(name = "circle_id")
    val circleId: String,
    @ColumnInfo(name = "ordered_at")
    val orderedAt: String,
)

data class CircleName(val name: String)
