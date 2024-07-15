package one.mixin.android.vo

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "inscription_collections")
data class InscriptionCollection(
    @PrimaryKey
    @ColumnInfo(name = "collection_hash")
    @SerializedName("collection_hash")
    val collectionHash: String,
    @ColumnInfo(name = "supply")
    @SerializedName("supply")
    val supply: String,
    @ColumnInfo(name = "unit")
    @SerializedName("unit")
    val unit: String,
    @ColumnInfo(name = "symbol")
    @SerializedName("symbol")
    val symbol: String,
    @ColumnInfo(name = "name")
    @SerializedName("name")
    val name: String,
    @ColumnInfo(name = "icon_url")
    @SerializedName("icon_url")
    val iconURL: String,
    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    val createdAt: String,
    @ColumnInfo(name = "updated_at")
    @SerializedName("updated_at")
    val updatedAt: String,
    @ColumnInfo(name = "description")
    @SerializedName("description")
    val description: String?,
    @ColumnInfo(name = "kernel_asset_id")
    @SerializedName("kernel_asset_id")
    val kernelAssetId: String?,
) : Parcelable
