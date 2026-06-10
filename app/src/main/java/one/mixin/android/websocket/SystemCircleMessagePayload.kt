package one.mixin.android.websocket

import com.google.gson.annotations.SerializedName

data class SystemCircleMessagePayload(
    @SerializedName("action")
    val action: String,
    @SerializedName("circle_id")
    val circleId: String,
    @SerializedName("conversation_id")
    val conversationId: String?,
    @SerializedName("user_id")
    val userId: String?,
)

enum class SystemCircleMessageAction { CREATE, DELETE, UPDATE, ADD, REMOVE }
