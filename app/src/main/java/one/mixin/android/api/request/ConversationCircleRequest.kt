package one.mixin.android.api.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ConversationCircleRequest(
    @Json(name = "circle_id")
    val circleId: String,
    @Json(name = "action")
    val action: String
)
