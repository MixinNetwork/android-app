package one.mixin.android.api.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

data class CircleConversationPayload(
    val conversationId: String,
    val userId: String? = null
)

@JsonClass(generateAdapter = true)
data class CircleConversationRequest(
    @Json(name = "conversation_id")
    val conversationId: String,
    @Json(name = "user_id")
    val userId: String? = null,
    @Json(name = "action")
    val action: String
)
