package one.mixin.android.crypto

import com.google.gson.annotations.SerializedName
import one.mixin.android.util.GsonHelper

class ProvisionMessage(
    @SerializedName("identity_key_public")
    val identityKeyPublic: ByteArray,
    @SerializedName("identity_key_private")
    val identityKeyPrivate: ByteArray,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("session_id")
    val sessionId: String,
    @SerializedName("provisioning_code")
    val provisioningCode: String,
    @SerializedName("platform")
    val platform: String = "Android",
) {
    fun toByteArray(): ByteArray {
        return GsonHelper.customGson.toJson(this).toByteArray()
    }
}

class ProvisionEnvelope(
    @SerializedName("public_key")
    val publicKey: ByteArray,
    @SerializedName("body")
    val body: ByteArray,
) {
    fun toByteArray(): ByteArray {
        return GsonHelper.customGson.toJson(this).toByteArray()
    }
}
