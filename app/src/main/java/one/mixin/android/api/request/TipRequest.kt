package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

class TipRequest(
    @SerializedName("device_id")
    val deviceId: String,
    @SerializedName("seed_base64")
    val seedBase64: String?
)

class TipSecretRequest(
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

enum class TipSecretAction {
    READ, UPDATE,
}
