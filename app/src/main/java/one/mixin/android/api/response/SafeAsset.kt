package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

data class SafeAsset(
    @SerializedName("asset_id")
    val assetId: String,
    @SerializedName("mixin_asset_id")
    val mixinAssetId: String,
    @SerializedName("account_id")
    val accountId: String,
    @SerializedName("address")
    val address: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("symbol")
    val symbol: String,
    @SerializedName("decimal")
    val decimal: Int,
    @SerializedName("balance")
    val balance: String,
    @SerializedName("price_usd")
    val priceUsd: String,
    @SerializedName("icon_url")
    val iconUrl: String,
    @SerializedName("recover_amount")
    val recoverAmount: String,
)
