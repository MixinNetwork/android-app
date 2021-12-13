package one.mixin.android.api
import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class SignedPreKey(
    keyId: Int,
    pubKey: String?,
    @SerializedName("signature")
    @Json(name = "signature")
    val signature: String
) : OneTimePreKey(keyId, pubKey)
