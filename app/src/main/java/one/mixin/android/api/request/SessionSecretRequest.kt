package one.mixin.android.api.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SessionSecretRequest(
    @Json(name = "session_secret")
    val sessionSecret: String
)
