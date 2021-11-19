package one.mixin.android.websocket

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SystemCircleMessagePayload(
    @SerializedName("action")
    @Json(name = "action")
    val action: String,
    @SerializedName("circle_id")
    @Json(name = "circle_id")
    val circleId: String,
    @SerializedName("conversation_id")
    @Json(name = "conversation_id")
    val conversationId: String?,
    @SerializedName("user_id")
    @Json(name = "user_id")
    val userId: String?
)

enum class SystemCircleMessageAction { CREATE, DELETE, UPDATE, ADD, REMOVE }
