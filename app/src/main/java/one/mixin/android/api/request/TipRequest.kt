package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

class TipRequest(
    @SerializedName("device_id")
    val deviceId: String,
    @SerializedName("seed_base64")
    val seedBase64: String?
)
