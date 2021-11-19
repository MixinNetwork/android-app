package one.mixin.android.websocket

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SystemUserMessagePayload(
    @SerializedName("action")
    @Json(name = "action")
    val action: String,
    @SerializedName("user_id")
    @Json(name = "user_id")
    val userId: String
)

enum class SystemUserMessageAction { UPDATE }
