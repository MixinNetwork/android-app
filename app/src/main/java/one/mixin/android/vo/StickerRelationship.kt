package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.google.gson.annotations.SerializedName

@Entity(
    tableName = "sticker_relationships",
    primaryKeys = ["album_id", "sticker_id"]
)
data class StickerRelationship(
    @SerializedName("album_id")
    @ColumnInfo(name = "album_id")
    val albumId: String,
    @SerializedName("sticker_id")
    @ColumnInfo(name = "sticker_id")
    val stickerId: String
)
