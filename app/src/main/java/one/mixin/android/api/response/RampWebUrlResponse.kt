package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

data class RampWebUrlResponse(
    @SerializedName("url")
    val url: String
)
