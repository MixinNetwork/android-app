package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Entity(
    tableName = "favorite_apps",
    primaryKeys = ["app_id", "user_id"]
)
@JsonClass(generateAdapter = true)
data class FavoriteApp(

    @Json(name ="app_id")
    @ColumnInfo(name = "app_id")
    val appId: String,

    @Json(name ="user_id")
    @ColumnInfo(name = "user_id")
    val userId: String,

    @Json(name ="created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: String
)
