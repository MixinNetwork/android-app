package one.mixin.android.vo.market

import com.google.gson.annotations.SerializedName

class GlobalMarket(
    @SerializedName("market_cap")
    val marketCap: String,
    @SerializedName("market_cap_change_percentage")
    val marketCapChangePercentage: String,
    @SerializedName("volume")
    val volume: String,
    @SerializedName("volume_change_percentage")
    val volumeChangePercentage: String,
    @SerializedName("dominance")
    val dominance: String,
    @SerializedName("dominance_percentage")
    val dominancePercentage: String,
)
