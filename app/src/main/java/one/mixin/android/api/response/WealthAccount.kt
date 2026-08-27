package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

data class WealthProduct(
    @SerializedName("production_id")
    val productionId: String,
    @SerializedName("asset_id")
    val assetId: String,
    @SerializedName("chain_id")
    val chainId: String,
    @SerializedName("icon_url")
    val iconUrl: String,
    @SerializedName("annual_rates")
    val annualRates: List<String> = emptyList(),
    @SerializedName("account")
    val account: WealthAccountSummary,
)

data class WealthAccountSummary(
    @SerializedName("total_principal")
    val totalPrincipal: String,
    @SerializedName("total_earnings")
    val totalEarnings: String,
    @SerializedName("redeemable_earnings")
    val redeemableEarnings: String,
)
