package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserSession(
    @SerializedName("user_id")
    @Json(name = "user_id")
    val userId: String,
    @SerializedName("session_id")
    @Json(name = "session_id")
    val sessionId: String,
    @SerializedName("platform")
    @Json(name = "platform")
    val platform: String?,
    @SerializedName("public_key")
    @Json(name = "public_key")
    val publicKey: String?,
)
