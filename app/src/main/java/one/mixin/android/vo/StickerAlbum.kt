package one.mixin.android.vo

import androidx.recyclerview.widget.DiffUtil
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Entity(tableName = "sticker_albums")
data class StickerAlbum(
    @PrimaryKey
    @SerializedName("album_id")
    @ColumnInfo(name = "album_id")
    val albumId: String,
    @SerializedName("name")
    @ColumnInfo(name = "name")
    val name: String,
    @SerializedName("icon_url")
    @ColumnInfo(name = "icon_url")
    val iconUrl: String,
    @SerializedName("created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @SerializedName("update_at")
    @ColumnInfo(name = "update_at")
    val updateAt: String,
    @SerializedName("user_id")
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "category")
    val category: String,
    @ColumnInfo(name = "description")
    val description: String,
    @ColumnInfo(name = "banner")
    val banner: String?,
    @Expose
    @ColumnInfo(name = "ordered_at", defaultValue = "0")
    var orderedAt: String = "0",
    @Expose
    @ColumnInfo(name = "added", defaultValue = "0")
    var added: Boolean = false,
) {
    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<StickerAlbum>() {
            override fun areItemsTheSame(oldItem: StickerAlbum, newItem: StickerAlbum) =
                oldItem.albumId == newItem.albumId

            override fun areContentsTheSame(oldItem: StickerAlbum, newItem: StickerAlbum) =
                oldItem.added == newItem.added &&
                    oldItem.orderedAt == newItem.orderedAt
        }
    }
}

data class StickerAlbumOrder(
    @ColumnInfo(name = "album_id")
    val albumId: String,
    @ColumnInfo(name = "ordered_at")
    val orderedAt: String?,
)

data class StickerAlbumAdded(
    @ColumnInfo(name = "album_id")
    val albumId: String,
    @ColumnInfo(name = "added")
    val added: Boolean?,
)
