package one.mixin.android.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import one.mixin.android.crypto.Base64
import one.mixin.android.extension.getDeviceId
import org.whispersystems.libsignal.IdentityKey
import org.whispersystems.libsignal.InvalidKeyException
import org.whispersystems.libsignal.ecc.Curve
import org.whispersystems.libsignal.ecc.ECPublicKey
import org.whispersystems.libsignal.state.PreKeyBundle
import java.io.IOException

@JsonClass(generateAdapter = true)
data class SignalKey(
    @Json(name = "identity_key")
    var identityKey: String,
    @Json(name = "signed_pre_key")
    var signedPreKey: SignedPreKey,
    @Json(name = "one_time_pre_key")
    var preKey: OneTimePreKey,
    @Json(name = "registration_id")
    var registrationId: Int,
    @Json(name = "user_id")
    val userId: String?,
    @Json(name = "session_id")
    val sessionId: String?
) {
    fun getPreKeyPublic(): ECPublicKey? {
        if (preKey.pubKey.isNullOrEmpty()) {
            return null
        }
        return try {
            Curve.decodePoint(Base64.decode(preKey.pubKey), 0)
        } catch (e: InvalidKeyException) {
            null
        } catch (e: IOException) {
            null
        }
    }

    fun getIdentity() = IdentityKey(Base64.decode(identityKey), 0)

    fun getSignedPreKeyPublic(): ECPublicKey? {
        if (signedPreKey.pubKey.isNullOrEmpty()) {
            return null
        }
        return try {
            Curve.decodePoint(Base64.decode(signedPreKey.pubKey), 0)
        } catch (e: InvalidKeyException) {
            null
        } catch (e: IOException) {
            null
        }
    }

    fun getSignedSignature() = Base64.decode(signedPreKey.signature)!!
}

fun createPreKeyBundle(key: SignalKey): PreKeyBundle {
    return PreKeyBundle(
        key.registrationId,
        key.sessionId.getDeviceId(),
        key.preKey.keyId,
        key.getPreKeyPublic(),
        key.signedPreKey.keyId,
        key.getSignedPreKeyPublic(),
        key.getSignedSignature(),
        key.getIdentity()
    )
}
