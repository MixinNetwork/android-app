package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

data class EarnProduct(
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
    val account: EarnAccountSummary,
)

data class EarnAccountSummary(
    @SerializedName("total_principal")
    val totalPrincipal: String,
    @SerializedName("total_earnings")
    val totalEarnings: String,
    @SerializedName("yesterday_earnings")
    val yesterdayEarnings: String = "0",
)
