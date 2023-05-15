package one.mixin.android.ui.transfer.vo

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import one.mixin.android.MixinApplication
import one.mixin.android.extension.getDeviceId

@Parcelize
@Serializable
data class TransferCommand(
    @SerialName("action")
    val action: String,
    @SerialName("ip")
    val ip: String? = null,
    @SerialName("port")
    val port: Int? = null,
    @SerializedName("secret_key")
    @SerialName("secret_key")
    val secretKey: String? = null,
    @SerialName("code")
    val code: Int? = null,
    @SerialName("total")
    val total: Long? = null,
    @SerializedName("user_id")
    @SerialName("user_id")
    val userId: String? = null,
    @SerialName("progress")
    val progress: Float? = null, // 0~100
    @SerialName("type")
    val type: String? = null,
    @SerialName("primary_id")
    val primaryId: String? = null,
    @SerialName("assistance_id")
    val assistanceId: String? = null,
    @SerialName("version")
    @EncodeDefault
    val version: Int = CURRENT_TRANSFER_VERSION,
    @SerializedName("device_id")
    @SerialName("device_id")
    @EncodeDefault
    val deviceId: String = MixinApplication.appContext.getDeviceId(),
    @SerialName("platform")
    @EncodeDefault
    val platform: String = "Android",
) : Parcelable

const val CURRENT_TRANSFER_VERSION = 1
