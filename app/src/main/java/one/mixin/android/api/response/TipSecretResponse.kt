package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

class TipSecretResponse(
    @SerializedName("seed_base64")
    val seedBase64: String? = null,
)
