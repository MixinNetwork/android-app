package one.mixin.android.websocket

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SystemUserMessagePayload(
    @Json(name = "action")
    val action: String,
    @Json(name ="user_id")
    val userId: String
)

enum class SystemUserMessageAction { UPDATE }
