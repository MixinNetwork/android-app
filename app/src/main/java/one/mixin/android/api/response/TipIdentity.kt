package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

class TipIdentity(
    @SerializedName("seed_base64")
    val seedBase64: String
)
