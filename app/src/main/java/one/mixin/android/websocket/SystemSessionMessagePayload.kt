package one.mixin.android.websocket

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SystemSessionMessagePayload(
    @Json(name = "action")
    val action: String,
    @Json(name = "user_id")
    val userId: String,
    @Json(name = "session_id")
    val sessionId: String,
    @Json(name = "public_key")
    val publicKey: String?
)

enum class SystemSessionMessageAction { PROVISION, DESTROY }
