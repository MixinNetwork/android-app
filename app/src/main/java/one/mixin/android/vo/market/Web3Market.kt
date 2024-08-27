package one.mixin.android.vo.market

import com.google.gson.annotations.SerializedName

data class Web3Market(
    @SerializedName("coin_id") val coinId: String,
    @SerializedName("name") val name: String,
    @SerializedName("symbol") val symbol: String,
    @SerializedName("icon_url") val iconUrl: String,
    @SerializedName("current_price") val currentPrice: String,
    @SerializedName("price_change_percentage_24h") val priceChangePercentage24h: String,
    @SerializedName("market_cap") val marketCap: String,
    @SerializedName("market_cap_rank") val marketCapRank: String,
    @SerializedName("total_volume") val totalVolume: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("asset_ids") val assetIds: List<String>,
    @SerializedName("sparkline_in_7d") val sparklineIn7d: String
)