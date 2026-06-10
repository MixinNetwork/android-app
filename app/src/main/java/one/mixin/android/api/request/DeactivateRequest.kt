package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

data class DeactivateRequest(
    @SerializedName("pin_base64")
    val pin: String,
    @SerializedName("verification_id")
    val verificationId: String? = null,
)
