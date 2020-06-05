package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.google.gson.annotations.SerializedName

@Entity(
    tableName = "favorite_apps",
    primaryKeys = ["app_id", "user_id"]
)
data class FavoriteApp(

    @SerializedName("app_id")
    @ColumnInfo(name = "app_id")
    val appId: String,

    @SerializedName("user_id")
    @ColumnInfo(name = "user_id")
    val userId: String,

    @SerializedName("created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: String
)
