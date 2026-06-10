package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

class RouteInstrumentRequest(
    @SerializedName("token")
    val token: String,
    @SerializedName("name")
    val name: String?,
    @SerializedName("phone")
    val phone: String?,
)
