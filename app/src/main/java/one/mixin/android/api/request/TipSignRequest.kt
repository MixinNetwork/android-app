package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

class TipSignRequest(
    @SerializedName("signature")
    val signature: String,
    @SerializedName("identity")
    val identity: String,
    @SerializedName("data")
    val data: String,
)
