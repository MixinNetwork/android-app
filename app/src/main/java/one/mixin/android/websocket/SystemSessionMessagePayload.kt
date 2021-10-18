package one.mixin.android.websocket

import com.google.gson.annotations.SerializedName

data class SystemSessionMessagePayload(
    @SerializedName("action")
    val action: String,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("session_id")
    val sessionId: String,
    @SerializedName("public_key")
    val publicKey: String?
)

enum class SystemSessionMessageAction { PROVISION, DESTROY }
