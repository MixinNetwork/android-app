package one.mixin.android.websocket

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SystemConversationMessagePayload(
    @Json(name = "action")
    val action: String,
    @Json(name = "participant_id")
    val participantId: String?,
    @Json(name = "user_id")
    val userId: String?,
    @Json(name = "role")
    val role: String?
)

enum class SystemConversationAction { JOIN, EXIT, ADD, REMOVE, CREATE, UPDATE, ROLE }
