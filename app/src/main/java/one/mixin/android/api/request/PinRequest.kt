package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

data class PinRequest(
    @SerializedName("pin")
    val pin: String,
    @SerializedName("old_pin")
    val oldPin: String? = null,
    @SerializedName("salt_base64")
    var salt: String? = null,
    @SerializedName("old_salt_base64")
    var oldSalt: String? = null,
    @SerializedName("timestamp")
    val timestamp: Long? = null,
)
