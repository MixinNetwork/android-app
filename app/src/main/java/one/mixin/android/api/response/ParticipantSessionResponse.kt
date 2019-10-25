package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

class ParticipantSessionResponse(
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("session_id")
    val sessionId: String,
    @SerializedName("platform")
    val platform: String
)
