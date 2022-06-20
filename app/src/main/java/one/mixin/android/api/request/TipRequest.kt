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
    @SerializedName("signature_base64")
    val signatureBase64: String,
    @SerializedName("timestamp")
    val timestamp: String,
)

enum class TipSecretAction {
    READ, UPDATE,
}
