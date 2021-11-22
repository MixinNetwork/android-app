package one.mixin.android.vo

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@SuppressLint("ParcelCreator")
@Parcelize
@Entity(tableName = "addresses")
@JsonClass(generateAdapter = true)
data class Address(
    @PrimaryKey
    @ColumnInfo(name = "address_id")
    @Json(name = "address_id")
    val addressId: String,
    @ColumnInfo(name = "type")
    val type: String,
    @ColumnInfo(name = "asset_id")
    @Json(name = "asset_id")
    val assetId: String,
    @ColumnInfo(name = "destination")
    @Json(name = "destination")
    val destination: String,
    @ColumnInfo(name = "label")
    val label: String,
    @ColumnInfo(name = "updated_at")
    @Json(name = "updated_at")
    val updatedAt: String,
    @ColumnInfo(name = "reserve")
    val reserve: String,
    @ColumnInfo(name = "fee")
    val fee: String,
    @ColumnInfo(name = "tag")
    @Json(name = "tag")
    val tag: String?,
    @ColumnInfo(name = "dust")
    @Json(name = "dust")
    val dust: String?
) : Parcelable

fun Address.displayAddress(): String {
    return if (!tag.isNullOrEmpty()) {
        "$destination:$tag"
    } else {
        destination
    }
}
