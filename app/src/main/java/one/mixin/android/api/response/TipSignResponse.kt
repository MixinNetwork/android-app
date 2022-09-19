package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

data class TipSignResponse(
    @SerializedName("data")
    val data: TipSignData,
    @SerializedName("signature")
    val signature: String,
)

data class TipSignData(
    @SerializedName("cipher")
    val cipher: String,
)
