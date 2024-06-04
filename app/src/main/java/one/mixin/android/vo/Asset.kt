package one.mixin.android.vo

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import one.mixin.android.crypto.sha3Sum256
import one.mixin.android.extension.isUUID
import one.mixin.android.vo.safe.Token
import java.math.BigDecimal

@SuppressLint("ParcelCreator")
@Parcelize
@Entity(tableName = "assets")
@Serializable
data class Asset(
    @PrimaryKey
    @ColumnInfo(name = "asset_id")
    @SerializedName("asset_id")
    @SerialName("asset_id")
    val assetId: String,
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
    @ColumnInfo(name = "balance")
    @SerializedName("balance")
    @SerialName("balance")
    val balance: String,
    @ColumnInfo(name = "destination")
    @SerializedName("destination")
    @SerialName("destination")
    val destination: String,
    @ColumnInfo(name = "tag")
    @SerializedName("tag")
    @SerialName("tag")
    val tag: String?,
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
    val assetKey: String?,
    @ColumnInfo(name = "reserve")
    @SerializedName("reserve")
    @SerialName("reserve")
    val reserve: String?,
    @ColumnInfo(name = "deposit_entries")
    @SerializedName("deposit_entries")
    @SerialName("deposit_entries")
    val depositEntries: List<OldDepositEntry>?,
    @ColumnInfo(name = "withdrawal_memo_possibility")
    @SerializedName("withdrawal_memo_possibility")
    @SerialName("withdrawal_memo_possibility")
    val withdrawalMemoPossibility: WithdrawalMemoPossibility?,
) : Parcelable

data class PriceAndChange(
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

fun Asset.toAssetItem(chainIconUrl: String? = null): AssetItem =
    AssetItem(
        assetId,
        symbol,
        name,
        iconUrl,
        balance,
        destination,
        depositEntries,
        tag,
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
        reserve,
        withdrawalMemoPossibility,
    )

fun Asset.toTopAssetItem(chainIconUrl: String?) =
    TopAssetItem(
        assetId,
        symbol,
        name,
        iconUrl,
        chainId,
        chainIconUrl,
        assetKey,
        priceUsd,
        changeUsd,
        null,
    )

fun Token?.priceUSD(): BigDecimal =
    if (this == null) {
        BigDecimal.ZERO
    } else {
        if (priceUsd == "0") {
            BigDecimal.ZERO
        } else {
            BigDecimal(priceUsd)
        }
    }

fun assetIdToAsset(assetId: String): String {
    assert(assetId.isUUID())
    return assetId.sha3Sum256()
        .joinToString("") { "%02x".format(it) }
}
