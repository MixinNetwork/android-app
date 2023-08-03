package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

class PayTokenResponse(
    @SerializedName("type")
    val type: String,
    @SerializedName("token")
    val token: String,
    @SerializedName("expires_on")
    val expiresOn: String,
)
