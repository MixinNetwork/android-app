package one.mixin.android.vo.market

import com.google.gson.annotations.SerializedName

data class PriceResponse(
    @SerializedName("current_price")
    val currentPrice: String,
    @SerializedName("market_cap")
    val marketCap: String,
    @SerializedName("market_cap_rank")
    val marketCapRank: String,
    @SerializedName("total_volume")
    val totalVolume: String,
    @SerializedName("high_24h")
    val high24h: String,
    @SerializedName("low_24h")
    val low24h: String,
    @SerializedName("price_change_24h")
    val priceChange24h: String,
    @SerializedName("price_change_percentage_24h")
    val priceChangePercentage24h: String,
    @SerializedName("market_cap_change_24h")
    val marketCapChange24h: String,
    @SerializedName("market_cap_change_percentage_24h")
    val marketCapChangePercentage24h: String,
    @SerializedName("circulating_supply")
    val circulatingSupply: String,
    @SerializedName("total_supply")
    val totalSupply: String,
    @SerializedName("max_supply")
    val maxSupply: String,
    @SerializedName("ath")
    val ath: String,
    @SerializedName("ath_change_percentage")
    val athChangePercentage: String,
    @SerializedName("ath_date")
    val athDate: String,
    @SerializedName("atl")
    val atl: String,
    @SerializedName("atl_change_percentage")
    val atlChangePercentage: String,
    @SerializedName("atl_date")
    val atlDate: String
)