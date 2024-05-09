package one.mixin.android.vo.safe

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("ParcelCreator")
@Parcelize
@Entity(
    tableName = "tokens", indices = [
        Index(value = arrayOf("kernel_asset_id")),
        Index(value = arrayOf("collection_hash"))]
)
@Serializable
data class Token(
    @PrimaryKey
    @ColumnInfo(name = "asset_id")
    @SerializedName("asset_id")
    @SerialName("asset_id")
    val assetId: String,
    @ColumnInfo(name = "kernel_asset_id")
    @SerializedName("kernel_asset_id")
    @SerialName("kernel_asset_id")
    val asset: String,
    @ColumnInfo(name = "symbol")
    @SerializedName("symbol")
    @SerialName("symbol")
    val symbol: String,
    @ColumnInfo(name = "name")
    @SerializedName("name")
    @SerialName("name")
    val name: String,
    @ColumnInfo(name = "icon_url")
    @SerializedName("icon_url")
    @SerialName("icon_url")
    val iconUrl: String,
    @ColumnInfo(name = "price_btc")
    @SerializedName("price_btc")
    @SerialName("price_btc")
    val priceBtc: String,
    @ColumnInfo(name = "price_usd")
    @SerializedName("price_usd")
    @SerialName("price_usd")
    val priceUsd: String,
    @ColumnInfo(name = "chain_id")
    @SerializedName("chain_id")
    @SerialName("chain_id")
    val chainId: String,
    @ColumnInfo(name = "change_usd")
    @SerializedName("change_usd")
    @SerialName("change_usd")
    val changeUsd: String,
    @ColumnInfo(name = "change_btc")
    @SerializedName("change_btc")
    @SerialName("change_btc")
    val changeBtc: String,
    @ColumnInfo(name = "confirmations")
    @SerializedName("confirmations")
    @SerialName("confirmations")
    val confirmations: Int,
    @ColumnInfo(name = "asset_key")
    @SerializedName("asset_key")
    @SerialName("asset_key")
    val assetKey: String,
    @ColumnInfo(name = "dust")
    @SerializedName("dust")
    @SerialName("dust")
    val dust: String,
    @SerializedName("collection_hash")
    @ColumnInfo(name = "collection_hash")
    @SerialName("collection_hash")
    val collectionHash: String?,
) : Parcelable

data class TokenPriceAndChange(
    @ColumnInfo(name = "asset_id")
    val assetId: String,
    @ColumnInfo(name = "price_btc")
    val priceBtc: String,
    @ColumnInfo(name = "price_usd")
    val priceUsd: String,
    @ColumnInfo(name = "change_usd")
    val changeUsd: String,
    @ColumnInfo(name = "change_btc")
    val changeBtc: String,
)

fun Token.toAssetItem(chainIconUrl: String? = null): TokenItem =
    TokenItem(
        assetId,
        symbol,
        name,
        iconUrl,
        "0",
        priceBtc,
        priceUsd,
        chainId,
        changeUsd,
        changeBtc,
        false,
        confirmations,
        chainIconUrl,
        null,
        null,
        null,
        assetKey,
        dust,
        null,
        collectionHash
    )
