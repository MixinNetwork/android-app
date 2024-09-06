package one.mixin.android.vo

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@SuppressLint("ParcelCreator")
@Parcelize
@Entity(tableName = "addresses")
data class Address(
    @PrimaryKey
    @ColumnInfo(name = "address_id")
    @SerializedName("address_id")
    val addressId: String,
    @ColumnInfo(name = "type")
    val type: String,
    @ColumnInfo(name = "asset_id")
    @SerializedName("asset_id")
    val assetId: String,
    @ColumnInfo(name = "destination")
    @SerializedName("destination")
    val destination: String,
    @ColumnInfo(name = "label")
    val label: String,
    @ColumnInfo(name = "updated_at")
    @SerializedName("updated_at")
    val updatedAt: String,
    @ColumnInfo(name = "tag")
    @SerializedName("tag")
    val tag: String?,
    @ColumnInfo(name = "dust")
    @SerializedName("dust")
    val dust: String?
) : Parcelable


fun Address.displayAddress(): String {
    return if (!tag.isNullOrEmpty()) {
        "$destination:$tag"
    } else {
        destination
    }
}
