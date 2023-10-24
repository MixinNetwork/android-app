package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    @SerializedName("public_key")
    val publicKey: String,
    @SerializedName("signature")
    val signature: String,
)
