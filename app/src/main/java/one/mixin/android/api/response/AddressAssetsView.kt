package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class AddressAssetsView(
    @SerializedName("address")
    val address: String,
    @SerializedName("assets")
    val assets: List<AssetView>
)

data class AssetView(
    @SerializedName("asset_id")
    val assetId: String,
    @SerializedName("chain_id")
    val chainId: String,
    @SerializedName("asset_key")
    val assetKey: String,
    @SerializedName("kernel_asset_id")
    val kernelAssetId: String,
    @SerializedName("symbol")
    val symbol: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("precision")
    val precision: Long,
    @SerializedName("icon_url")
    val iconUrl: String,
    @SerializedName("amount")
    val amount: String,
    @SerializedName("price_usd")
    val priceUSD: String,
    @SerializedName("change_usd")
    val changeUSD: String,
    @SerializedName("price_change_percentage_24h")
    val priceChangePercentage24h: String,
    @SerializedName("wallet_id")
    val walletId: String,
    @SerializedName("level")
    val assetLevel: Int
) {
    val value
        get() = (amount.toBigDecimalOrNull() ?: BigDecimal.ZERO) * (priceUSD.toBigDecimalOrNull() ?: BigDecimal.ZERO)
}