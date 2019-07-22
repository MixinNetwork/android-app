package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName
import one.mixin.android.api.OneTimePreKey
import one.mixin.android.api.SignedPreKey
import one.mixin.android.crypto.Base64
import org.whispersystems.libsignal.IdentityKey
import org.whispersystems.libsignal.state.PreKeyRecord
import org.whispersystems.libsignal.state.SignedPreKeyRecord

class SignalKeyRequest(ik: IdentityKey, spk: SignedPreKeyRecord, otp: List<PreKeyRecord>? = null) {
    @SerializedName("identity_key")
    val identityKey: String = Base64.encodeBytes(ik.serialize())
    @SerializedName("signed_pre_key")
    var signedPreKey: SignedPreKey
    @SerializedName("one_time_pre_keys")
    lateinit var oneTimePreKeys: ArrayList<OneTimePreKey>

    init {
        val publicKeyBase64 = Base64.encodeBytes(spk.keyPair.publicKey.serialize())
        val signatureBase64 = Base64.encodeBytes(spk.signature)
        signedPreKey = SignedPreKey(spk.id, publicKeyBase64, signatureBase64)
        if (otp != null) {
            oneTimePreKeys = ArrayList()
            otp.mapTo(oneTimePreKeys) { OneTimePreKey(it.id, Base64.encodeBytes(it.keyPair.publicKey.serialize())) }
        }
    }
}
