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
@Entity(tableName = "assets")
data class Asset(
    @PrimaryKey
    @ColumnInfo(name = "asset_id")
    @SerializedName("asset_id")
    val assetId: String,
    @ColumnInfo(name = "symbol")
    val symbol: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "icon_url")
    @SerializedName("icon_url")
    val iconUrl: String,
    @ColumnInfo(name = "balance")
    val balance: String,
    @SerializedName("public_key")
    @ColumnInfo(name = "public_key")
    val publicKey: String,
    @SerializedName("price_btc")
    @ColumnInfo(name = "price_btc")
    val priceBtc: String,
    @SerializedName("price_usd")
    @ColumnInfo(name = "price_usd")
    val priceUsd: String,
    @SerializedName("chain_id")
    @ColumnInfo(name = "chain_id")
    val chainId: String,
    @SerializedName("change_usd")
    @ColumnInfo(name = "change_usd")
    val changeUsd: String,
    @SerializedName("change_btc")
    @ColumnInfo(name = "change_btc")
    val changeBtc: String,
    @SerializedName("hidden")
    @ColumnInfo(name = "hidden")
    var hidden: Boolean?,
    @ColumnInfo(name = "confirmations")
    val confirmations: Int,
    @SerializedName("account_name")
    @ColumnInfo(name = "account_name")
    val accountName: String?,
    @SerializedName("account_memo")
    @ColumnInfo(name = "account_memo")
    val accountMemo: String?
) : Parcelable
