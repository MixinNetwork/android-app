package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

data class ProvisioningRequest(
    @SerializedName("secret")
    val secret: String,
    @SerializedName("pin_base64")
    val pin: String,
)
