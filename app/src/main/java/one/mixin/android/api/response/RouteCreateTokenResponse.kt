package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

class RouteCreateTokenResponse(
    @SerializedName("type")
    val type: String,
    @SerializedName("token")
    val token: String,
    @SerializedName("token_format")
    val tokenFormat: String,
    @SerializedName("scheme")
    val scheme: String,
    @SerializedName("issuer_country")
    val issuerCountry: String,
)
