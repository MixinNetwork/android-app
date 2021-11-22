package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Entity(
    tableName = "sticker_relationships",
    primaryKeys = ["album_id", "sticker_id"]
)
@JsonClass(generateAdapter = true)
data class StickerRelationship(
    @Json(name = "album_id")
    @ColumnInfo(name = "album_id")
    val albumId: String,
    @Json(name = "sticker_id")
    @ColumnInfo(name = "sticker_id")
    val stickerId: String
)
