package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName
import one.mixin.android.api.ResponseError

data class TipSignResponse(
    @SerializedName("data")
    val data: TipSignData,
    @SerializedName("signature")
    val signature: String,
    @SerializedName("error")
    var error: ResponseError? = null,
)

data class TipSignData(
    @SerializedName("cipher")
    val cipher: String,
)
