package one.mixin.android.api.response

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ProvisioningResponseCode(val code: String)

@JsonClass(generateAdapter = true)
data class ProvisioningResponse(val device_id: String, val description: String)
