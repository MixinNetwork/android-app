package one.mixin.android.api.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class ConversationRequest(
    @Json(name = "conversation_id")
    val conversationId: String,
    @Json(name = "category")
    val category: String? = null,
    @Json(name = "name")
    val name: String? = null,
    @Json(name = "icon_base64")
    val iconBase64: String? = null,
    @Json(name = "announcement")
    val announcement: String? = null,
    @Json(name = "participants")
    val participants: List<ParticipantRequest>? = null,
    @Json(name = "duration")
    val duration: Long? = null
)
