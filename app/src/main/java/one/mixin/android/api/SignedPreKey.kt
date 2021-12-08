package one.mixin.android.api
import com.google.gson.annotations.SerializedName

class SignedPreKey(
    keyId: Int,
    pubKey: String,
    @SerializedName("signature")
    val signature: String
) : OneTimePreKey(keyId, pubKey)
