package one.mixin.android.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SessionSecretResponse(
    @Json(name = "pin_token")
    val pinToken: String
)
