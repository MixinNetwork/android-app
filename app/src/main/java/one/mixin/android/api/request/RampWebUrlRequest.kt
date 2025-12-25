package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

data class RampWebUrlRequest(
    val amount: String,
    @SerializedName("asset_id")
    val assetId: String,
    val currency: String,
    val destination: String,
    val phone: String?,
    @SerializedName("phone_verified_at")
    val phoneVerifiedAt: String?,
)
