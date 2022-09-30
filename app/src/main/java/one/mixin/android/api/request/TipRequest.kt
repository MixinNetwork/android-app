package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

data class TipRequest(
    @SerializedName("device_id")
    val deviceId: String,
    @SerializedName("seed_base64")
    val seedBase64: String?
)

data class TipSecretRequest(
    @SerializedName("action")
    val action: String,
    @SerializedName("seed_base64")
    val seedBase64: String? = null,
    @SerializedName("secret_base64")
    val secretBase64: String? = null,
    @SerializedName("signature_base64")
    val signatureBase64: String,
    @SerializedName("timestamp")
    val timestamp: Long,
)

data class TipSecretReadRequest(
    @SerializedName("signature_base64")
    val signatureBase64: String,
    @SerializedName("timestamp")
    val timestamp: Long,
) {
    @SerializedName("action")
    val action: String = TipSecretAction.READ.name
}


enum class TipSecretAction {
    READ, UPDATE,
}
