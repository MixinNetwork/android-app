package one.mixin.android.vo

import androidx.recyclerview.widget.DiffUtil
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity(tableName = "stickers")
@Serializable
data class Sticker(
    @PrimaryKey
    @SerializedName("sticker_id")
    @SerialName("sticker_id")
    @ColumnInfo(name = "sticker_id")
    val stickerId: String,
    @SerializedName("album_id")
    @SerialName("album_id")
    @ColumnInfo(name = "album_id")
    val albumId: String?,
    @SerializedName("name")
    @SerialName("name")
    @ColumnInfo(name = "name")
    val name: String,
    @SerializedName("asset_url")
    @SerialName("asset_url")
    @ColumnInfo(name = "asset_url")
    val assetUrl: String,
    @SerializedName("asset_type")
    @SerialName("asset_type")
    @ColumnInfo(name = "asset_type")
    val assetType: String,
    @SerializedName("asset_width")
    @SerialName("asset_width")
    @ColumnInfo(name = "asset_width")
    val assetWidth: Int,
    @SerializedName("asset_height")
    @SerialName("asset_height")
    @ColumnInfo(name = "asset_height")
    val assetHeight: Int,
    @SerializedName("created_at")
    @SerialName("created_at")
    @ColumnInfo(name = "created_at")
    var createdAt: String,
    @SerializedName("last_use_at")
    @SerialName("last_use_at")
    @ColumnInfo(name = "last_use_at")
    var lastUseAt: String?,
) {
    companion object {
        const val STICKER_TYPE_JSON = "JSON"

        val DIFF_CALLBACK =
            object : DiffUtil.ItemCallback<Sticker>() {
                override fun areItemsTheSame(
                    oldItem: Sticker,
                    newItem: Sticker,
                ) =
                    oldItem.albumId == newItem.albumId

                override fun areContentsTheSame(
                    oldItem: Sticker,
                    newItem: Sticker,
                ) =
                    oldItem == newItem
            }
    }
}

fun Sticker.isLottie() = assetType.equals(Sticker.STICKER_TYPE_JSON, true)
