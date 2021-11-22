package one.mixin.android.api.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class ParticipantRequest(
    @Json(name ="user_id")
    val userId: String,
    @Json(name ="role")
    val role: String,
    @Json(name ="created_at")
    val createdAt: String? = null
)

enum class ParticipantAction { ADD, REMOVE, JOIN, EXIT, ROLE }
