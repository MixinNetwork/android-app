package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

data class ProvisioningResponseCode(
    @SerializedName("code")
    val code: String,
)

data class ProvisioningResponse(
    @SerializedName("device_id")
    val device_id: String,
    @SerializedName("description")
    val description: String,
)
