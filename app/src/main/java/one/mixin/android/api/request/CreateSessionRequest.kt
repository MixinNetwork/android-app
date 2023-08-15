package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

class CreateSessionRequest(
    @SerializedName("token")
    val token: String,
    @SerializedName("currency")
    val currency: String,
    @SerializedName("scheme")
    val scheme: String?,
    @SerializedName("asset_id")
    val assetId: String,
    @SerializedName("amount")
    var amount: Int,
)
