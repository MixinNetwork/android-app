package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

data class RampWebUrlRequest(
    @SerializedName("amount")
    val amount: String,
    @SerializedName("asset_id")
    val assetId: String,
    @SerializedName("currency")
    val currency: String,
    @SerializedName("destination")
    val destination: String,
    @SerializedName("phone")
    val phone: String?,
    @SerializedName("phone_verified_at")
    val phoneVerifiedAt: String?,
)
