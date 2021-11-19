package one.mixin.android.crypto

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import one.mixin.android.moshi.MoshiHelper.getTypeAdapter

@JsonClass(generateAdapter = true)
class ProvisionMessage(
    @SerializedName("identity_key_public")
    @Json(name = "identity_key_public")
    val identityKeyPublic: ByteArray,
    @SerializedName("identity_key_private")
    @Json(name = "identity_key_private")
    val identityKeyPrivate: ByteArray,
    @SerializedName("user_id")
    @Json(name = "user_id")
    val userId: String,
    @SerializedName("session_id")
    @Json(name = "session_id")
    val sessionId: String,
    @SerializedName("provisioning_code")
    @Json(name = "provisioning_code")
    val provisioningCode: String,
    @SerializedName("platform")
    @Json(name = "platform")
    val platform: String = "Android"
) {

    fun toByteArray(): ByteArray {
        return getTypeAdapter<ProvisionMessage>(ProvisionMessage::class.java).toJson(this).toByteArray()
    }
}

@JsonClass(generateAdapter = true)
class ProvisionEnvelope(
    @SerializedName("public_key")
    @Json(name = "public_key")
    val publicKey: ByteArray,
    @SerializedName("body")
    @Json(name = "body")
    val body: ByteArray
) {

    fun toByteArray(): ByteArray {
        return getTypeAdapter<ProvisionEnvelope>(ProvisionEnvelope::class.java).toJson(this).toByteArray()
    }
}
