package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

class RouteTokenRequest(
    @SerializedName("token")
    val token: String,
    @SerializedName("type")
    val type: String = "googlepay",
)
