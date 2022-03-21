package one.mixin.android.websocket

import com.google.gson.annotations.SerializedName

data class SystemConversationMessagePayload(
    @SerializedName("action")
    val action: String,
    @SerializedName("participant_id")
    val participantId: String?,
    @SerializedName("user_id")
    val userId: String?,
    @SerializedName("role")
    val role: String?,
    @SerializedName("expire_in")
    val expireIn: Long?
)

enum class SystemConversationAction { JOIN, EXIT, ADD, REMOVE, CREATE, UPDATE, ROLE, EXPIRE }
