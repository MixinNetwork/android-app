package one.mixin.android.api.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ProvisioningRequest(
    @Json(name = "secret")
    val secret: String
)
