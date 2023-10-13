package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

data class LogoutRequest(
    @SerializedName("session_id")
    val sessionId: String,
)
