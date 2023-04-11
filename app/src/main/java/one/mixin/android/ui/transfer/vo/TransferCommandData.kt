package one.mixin.android.ui.transfer.vo

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import one.mixin.android.MixinApplication
import one.mixin.android.extension.getDeviceId

@Parcelize
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
    val progress: Float? = null, // 0~100
    val version: Int = CURRENT_TRANSFER_VERSION,
    @SerializedName("device_id")
    val deviceId: String = MixinApplication.appContext.getDeviceId(),
    val platform: String = "Android",
) : Parcelable

const val CURRENT_TRANSFER_VERSION = 1
