package one.mixin.android.vo

import android.annotation.SuppressLint
import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

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
    @ColumnInfo(name = "public_key")
    @SerializedName("public_key")
    val publicKey: String,
    @ColumnInfo(name = "label")
    val label: String,
    @ColumnInfo(name = "updated_at")
    @SerializedName("updated_at")
    val updatedAt: String,
    @ColumnInfo(name = "reserve")
    val reserve: String,
    @ColumnInfo(name = "fee")
    val fee: String
) : Parcelable