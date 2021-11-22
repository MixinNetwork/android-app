package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Entity(tableName = "sticker_albums")
@JsonClass(generateAdapter = true)
data class StickerAlbum(
    @PrimaryKey
    @Json(name = "album_id")
    @ColumnInfo(name = "album_id")
    val albumId: String,
    @Json(name = "name")
    @ColumnInfo(name = "name")
    val name: String,
    @Json(name = "icon_url")
    @ColumnInfo(name = "icon_url")
    val iconUrl: String,
    @Json(name = "created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @Json(name = "update_at")
    @ColumnInfo(name = "update_at")
    val updateAt: String,
    @Json(name = "user_id")
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "category")
    val category: String,
    @ColumnInfo(name = "description")
    val description: String
)
