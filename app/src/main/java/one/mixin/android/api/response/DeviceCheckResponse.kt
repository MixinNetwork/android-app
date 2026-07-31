package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

class DeviceCheckResponse(
    @SerializedName("nonce")
    val nonce: String,
)
