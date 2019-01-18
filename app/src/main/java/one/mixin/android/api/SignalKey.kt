package one.mixin.android.api

import com.google.gson.annotations.SerializedName
import one.mixin.android.crypto.Base64
import one.mixin.android.crypto.SignalProtocol
import org.whispersystems.libsignal.IdentityKey
import org.whispersystems.libsignal.ecc.Curve
import org.whispersystems.libsignal.state.PreKeyBundle

data class SignalKey(
    @SerializedName("identity_key")
    var identityKey: String,
    @SerializedName("signed_pre_key")
    var signedPreKey: SignedPreKey,
    @SerializedName("one_time_pre_key")
    var preKey: OneTimePreKey,
    @SerializedName("registration_id")
    var registrationId: Int,
    @SerializedName("user_id")
    val userId: String?,
    @SerializedName("session_id")
    val sessionId: String
) {
    fun getPreKeyPublic() = Curve.decodePoint(Base64.decode(preKey.pubKey), 0)!!

    fun getIdentity() = IdentityKey(Base64.decode(identityKey), 0)

    fun getSignedPreKeyPublic() = Curve.decodePoint(Base64.decode(signedPreKey.pubKey), 0)!!

    fun getSignedSignature() = Base64.decode(signedPreKey.signature)!!
}

fun createPreKeyBundle(key: SignalKey): PreKeyBundle {
    return PreKeyBundle(
        key.registrationId,
        SignalProtocol.DEFAULT_DEVICE_ID,
        key.preKey.keyId,
        key.getPreKeyPublic(),
        key.signedPreKey.keyId,
        key.getSignedPreKeyPublic(),
        key.getSignedSignature(),
        key.getIdentity()
    )
}