package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

data class MarketFavoritesRequest(
    @SerializedName("market_ids")
    val marketIds: List<String>,
)
