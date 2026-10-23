package one.mixin.android.vo

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import com.google.gson.annotations.SerializedName

@Entity(
    tableName = "sticker_relationships",
    primaryKeys = ["album_id", "sticker_id"],
)
data class StickerRelationship(
    @SerializedName("album_id")
    @ColumnInfo(name = "album_id")
    val albumId: String,
    @SerializedName("sticker_id")
    @ColumnInfo(name = "sticker_id")
    val stickerId: String,
)
