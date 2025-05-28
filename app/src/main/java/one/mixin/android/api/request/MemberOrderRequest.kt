package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName
import one.mixin.android.Constants.AssetId.USDT_ASSET_ID

class MemberOrderRequest(
    val category: String ="SUB",
    val plan: String,
    val asset: String = USDT_ASSET_ID,
    val source: String = "mixin"
)
