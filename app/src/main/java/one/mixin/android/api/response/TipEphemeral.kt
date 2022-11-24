package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

class TipEphemeral(
    @SerializedName("type")
    val type: String,
    @SerializedName("device_id")
    val deviceId: String,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("seed_base64")
    val seedBase64: String,
    @SerializedName("created_at")
    val createdAt: String
)
