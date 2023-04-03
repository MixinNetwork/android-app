package one.mixin.android.ui.transfer.vo

import com.google.gson.annotations.SerializedName
import one.mixin.android.MixinApplication
import one.mixin.android.extension.getDeviceId

data class TransferCommandData(
    val action: String,
    val ip: String? = null,
    val port: Int? = null,
    @SerializedName("secret_key")
    val secretKey: String? = null,
    val code: Int? = null,
    val total: Long? = null,
    @SerializedName("user_id")
    val userId: String? = null,
    val version: Int = 1,
    @SerializedName("device_id")
    val deviceId: String = MixinApplication.appContext.getDeviceId(),
    val platform: String = "Android",
)
