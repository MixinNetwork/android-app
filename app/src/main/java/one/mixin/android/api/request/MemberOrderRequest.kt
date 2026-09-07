package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName
import one.mixin.android.Constants.AssetId.USDT_ASSET_ETH_ID

data class MemberOrderRequest(
    @SerializedName("category")
    val category: String = "SUB",
    @SerializedName("plan")
    val plan: String,
    @SerializedName("asset")
    val asset: String = USDT_ASSET_ETH_ID,
    @SerializedName("source")
    val source: String = "mixin",
    @SerializedName("fiat_source")
    val fiatSource: String? = null,
    @SerializedName("subscription_id")
    val subscriptionId: String? = null,
)
