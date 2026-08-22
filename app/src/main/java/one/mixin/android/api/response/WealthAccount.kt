package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

data class WealthProduct(
    @SerializedName("production_id")
    val productionId: String,
    @SerializedName("asset_id")
    val assetId: String,
    @SerializedName("chain_id")
    val chainId: String? = null,
    @SerializedName("asset_name")
    val assetName: String? = null,
    @SerializedName("asset_symbol")
    val assetSymbol: String? = null,
    @SerializedName("precision")
    val precision: Int? = null,
    @SerializedName("icon_url")
    val iconUrl: String? = null,
    @SerializedName("price_usd")
    val priceUsd: String? = null,
    @SerializedName("kind")
    val kind: String? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("start_at")
    val startAt: String? = null,
    @SerializedName("end_at")
    val endAt: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null,
    @SerializedName("annual_rates")
    val annualRates: List<String>? = null,
    @SerializedName("annual_rate_tiers")
    val annualRateTiers: List<WealthAnnualRateTier>? = null,
    @SerializedName("max_per_user")
    val maxPerUser: String? = null,
    @SerializedName("share_prices")
    val sharePrices: Map<String, String>? = null,
    @SerializedName("account")
    val account: WealthAccountSummary? = null,
)

data class WealthAnnualRateTier(
    @SerializedName("min_amount")
    val minAmount: String,
    @SerializedName("max_amount")
    val maxAmount: String,
    @SerializedName("rate")
    val rate: String,
)

data class WealthAccountSummary(
    @SerializedName("total_principal")
    val totalPrincipal: String? = null,
    @SerializedName("total_earnings")
    val totalEarnings: String? = null,
    @SerializedName("redeemable_earnings")
    val redeemableEarnings: String? = null,
)
