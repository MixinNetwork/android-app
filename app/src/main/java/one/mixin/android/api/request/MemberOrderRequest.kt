package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName
import one.mixin.android.Constants.AssetId.USDT_ASSET_ID

data class MemberOrderRequest(
    val category: String = "SUB",
    val plan: String,
    val asset: String = USDT_ASSET_ID,
    val source: String = "mixin",
    @SerializedName("fiat_source")
    val fiatSource: String? = null,
    @SerializedName("subscription_id")
    val subscriptionId: String? = null,
)
