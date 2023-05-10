package one.mixin.android.ui.transfer.vo

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransferScene(
    @SerialName("device_id")
    @SerializedName("device_id")
    val deviceId: String,
    @SerialName("start_time")
    @SerializedName("start_time")
    val startTime: Long,
    @SerialName("type")
    @SerializedName("type")
    val type: String,
    @SerialName("primary_id")
    @SerializedName("primary_id")
    val primaryId: String,
    @SerialName("assistance_id")
    @SerializedName("assistance_id")
    val assistanceId: String?,
) {
    companion object {
        fun from(
            deviceId: String?,
            startTime: Long?,
            type: String?,
            primaryId: String?,
            assistanceId: String?,
        ): TransferScene? {
            if (deviceId == null || startTime == null || type == null || primaryId == null) {
                return null
            }
            return TransferScene(
                deviceId = deviceId,
                startTime = startTime,
                type = type,
                primaryId = primaryId,
                assistanceId = assistanceId,
            )
        }
    }
}
