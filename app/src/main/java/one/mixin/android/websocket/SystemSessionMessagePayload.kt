package one.mixin.android.websocket

import com.google.gson.annotations.SerializedName

data class SystemSessionMessagePayload(
    @SerializedName("action")
    val action: String,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("session_id")
    val sessionId: String
)

enum class SystemSessionMessageAction { PROVISION, ADD, DESTROY }
