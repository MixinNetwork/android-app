package one.mixin.android.vo

import androidx.recyclerview.widget.DiffUtil
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Entity(tableName = "stickers")
@JsonClass(generateAdapter = true)
data class Sticker(
    @PrimaryKey
    @Json(name = "sticker_id")
    @ColumnInfo(name = "sticker_id")
    val stickerId: String,
    @Json(name = "album_id")
    @ColumnInfo(name = "album_id")
    val albumId: String?,
    @Json(name = "name")
    @ColumnInfo(name = "name")
    val name: String,
    @Json(name = "asset_url")
    @ColumnInfo(name = "asset_url")
    val assetUrl: String,
    @Json(name = "asset_type")
    @ColumnInfo(name = "asset_type")
    val assetType: String,
    @Json(name = "asset_width")
    @ColumnInfo(name = "asset_width")
    val assetWidth: Int,
    @Json(name = "asset_height")
    @ColumnInfo(name = "asset_height")
    val assetHeight: Int,
    @Json(name = "created_at")
    @ColumnInfo(name = "created_at")
    var createdAt: String,
    @ColumnInfo(name = "last_use_at")
    var lastUseAt: String?
) {
    companion object {
        const val STICKER_TYPE_JSON = "JSON"

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Sticker>() {
            override fun areItemsTheSame(oldItem: Sticker, newItem: Sticker) =
                oldItem.albumId == newItem.albumId

            override fun areContentsTheSame(oldItem: Sticker, newItem: Sticker) =
                oldItem == newItem
        }
    }
}

fun Sticker.isLottie() = assetType.equals(Sticker.STICKER_TYPE_JSON, true)
