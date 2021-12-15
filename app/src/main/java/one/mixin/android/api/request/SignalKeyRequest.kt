package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName
import one.mixin.android.api.OneTimePreKey
import one.mixin.android.api.SignedPreKey
import one.mixin.android.extension.base64Encode
import org.whispersystems.libsignal.IdentityKey
import org.whispersystems.libsignal.state.PreKeyRecord
import org.whispersystems.libsignal.state.SignedPreKeyRecord

class SignalKeyRequest(ik: IdentityKey, spk: SignedPreKeyRecord, otp: List<PreKeyRecord>? = null) {
    @SerializedName("identity_key")
    val identityKey: String = ik.serialize().base64Encode()
    @SerializedName("signed_pre_key")
    var signedPreKey: SignedPreKey
    @SerializedName("one_time_pre_keys")
    lateinit var oneTimePreKeys: ArrayList<OneTimePreKey>

    init {
        val publicKeyBase64 = spk.keyPair.publicKey.serialize().base64Encode()
        val signatureBase64 = spk.signature.base64Encode()
        signedPreKey = SignedPreKey(spk.id, publicKeyBase64, signatureBase64)
        if (otp != null) {
            oneTimePreKeys = ArrayList()
            otp.mapTo(oneTimePreKeys) { OneTimePreKey(it.id, it.keyPair.publicKey.serialize().base64Encode()) }
        }
    }
}
