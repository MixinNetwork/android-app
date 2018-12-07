package one.mixin.android.api.response

data class ProvisioningResponseCode(val code: String)

data class ProvisioningResponse(val device_id: String, val description: String)
