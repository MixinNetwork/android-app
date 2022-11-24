package one.mixin.android.api.response.signature

import com.google.gson.annotations.SerializedName

data class TipWatchResponse(
    @SerializedName("counter")
    val counter: Int,
    @SerializedName("genesis")
    val genesis: String
)
