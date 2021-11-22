package one.mixin.android.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserSession(
    @Json(name = "user_id")
    val userId: String,
    @Json(name = "session_id")
    val sessionId: String,
    @Json(name = "platform")
    val platform: String?,
    @Json(name ="public_key")
    val publicKey: String?,
)
