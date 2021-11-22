package one.mixin.android.websocket

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SystemCircleMessagePayload(
    @Json(name = "action")
    val action: String,
    @Json(name = "circle_id")
    val circleId: String,
    @Json(name = "conversation_id")
    val conversationId: String?,
    @Json(name = "user_id")
    val userId: String?
)

enum class SystemCircleMessageAction { CREATE, DELETE, UPDATE, ADD, REMOVE }
