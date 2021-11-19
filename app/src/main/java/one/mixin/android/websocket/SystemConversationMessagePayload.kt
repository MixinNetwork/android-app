package one.mixin.android.websocket

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SystemConversationMessagePayload(
    @SerializedName("action")
    @Json(name = "action")
    val action: String,
    @SerializedName("participant_id")
    @Json(name = "participant_id")
    val participantId: String?,
    @SerializedName("user_id")
    @Json(name = "user_id")
    val userId: String?,
    @SerializedName("role")
    @Json(name = "role")
    val role: String?
)

enum class SystemConversationAction { JOIN, EXIT, ADD, REMOVE, CREATE, UPDATE, ROLE }
