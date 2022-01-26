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
    val description: String,
    @ColumnInfo(name = "banner")
    val banner: String?,
    @ColumnInfo(name = "ordered_at", defaultValue = "0")
    var orderedAt: Int = 0,
    @ColumnInfo(name = "added", defaultValue = "0")
    var added: Boolean = false,
)

data class StickerAlbumOrder(
    @ColumnInfo(name = "album_id")
    val albumId: String,
    @ColumnInfo(name = "ordered_at")
    val orderedAt: Int,
)

data class StickerAlbumAdded(
    @ColumnInfo(name = "album_id")
    val albumId: String,
    @ColumnInfo(name = "added")
    val added: Boolean,
    @ColumnInfo(name = "ordered_at")
    var orderedAt: Int,
)
