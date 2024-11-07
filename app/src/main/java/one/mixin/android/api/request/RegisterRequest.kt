package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    @SerializedName("public_key")
    val publicKey: String,
    @SerializedName("signature")
    val signature: String,
    @SerializedName("pin_base64")
    val pin: String,
    @SerializedName("salt_base64")
    val salt: String,
    @SerializedName("salt_public_hex")
    val saltPublicHex: String,
    @SerializedName("salt_signature_hex")
    val saltSignatureHex: String,
)
