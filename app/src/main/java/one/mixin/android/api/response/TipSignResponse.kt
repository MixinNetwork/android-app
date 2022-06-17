package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

data class TipSignResponse(
    @SerializedName("data")
    val data: String,
    @SerializedName("signature")
    val signature: String,
)
