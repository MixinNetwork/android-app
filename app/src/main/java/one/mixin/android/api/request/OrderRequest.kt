package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

class OrderRequest(
    @SerializedName("currency")
    val currency: String,
    @SerializedName("scheme")
    val scheme: String?,
    @SerializedName("asset_id")
    val assetId: String,
    @SerializedName("amount")
    var amount: String,
    @SerializedName("asset_amount")
    var asstAmount: String,
    @SerializedName("instrument_id")
    val instrumentId: String? = null,
    @SerializedName("token")
    val token: String? = null,
    @SerializedName("type")
    val type: String? = null,
)
