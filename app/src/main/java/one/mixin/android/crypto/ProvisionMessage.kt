package one.mixin.android.crypto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import one.mixin.android.moshi.MoshiHelper.getTypeAdapter

@JsonClass(generateAdapter = true)
class ProvisionMessage(
    @Json(name = "identity_key_public")
    val identityKeyPublic: ByteArray,
    @Json(name = "identity_key_private")
    val identityKeyPrivate: ByteArray,
    @Json(name = "user_id")
    val userId: String,
    @Json(name = "session_id")
    val sessionId: String,
    @Json(name = "provisioning_code")
    val provisioningCode: String,
    @Json(name = "platform")
    val platform: String = "Android"
) {

    fun toByteArray(): ByteArray {
        return getTypeAdapter<ProvisionMessage>(ProvisionMessage::class.java).toJson(this).toByteArray()
    }
}

@JsonClass(generateAdapter = true)
class ProvisionEnvelope(
    @Json(name = "public_key")
    val publicKey: ByteArray,
    @Json(name = "body")
    val body: ByteArray
) {

    fun toByteArray(): ByteArray {
        return getTypeAdapter<ProvisionEnvelope>(ProvisionEnvelope::class.java).toJson(this).toByteArray()
    }
}
