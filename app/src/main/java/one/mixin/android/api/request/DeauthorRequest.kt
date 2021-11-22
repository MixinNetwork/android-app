package one.mixin.android.api.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class DeauthorRequest(
    @Json(name = "client_id")
    val clientId: String
)
