package one.mixin.android.ui.transfer.vo

import com.google.gson.annotations.SerializedName

data class TransferCommandData(
    @SerializedName("device_id")
    val deviceId: String,
    val action: String,
    val version: Int,
    val ip: String? = null,
    val port: Int? = null,
    @SerializedName("secret_key")
    val secretKey: String? = null,
    val code: Int? = null,
    val platform: String = "Android",
)
