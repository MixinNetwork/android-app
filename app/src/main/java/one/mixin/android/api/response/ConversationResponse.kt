package one.mixin.android.api.response

import com.squareup.moshi.Json
import one.mixin.android.api.request.ParticipantRequest
import one.mixin.android.vo.CircleConversation
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
open class ConversationResponse(
    @Json(name ="conversation_id")
    val conversationId: String,
    @Json(name ="name")
    val name: String,
    @Json(name ="category")
    val category: String,
    @Json(name ="creator_id")
    val creatorId: String,
    @Json(name ="icon_url")
    val iconUrl: String,
    @Json(name ="code_url")
    val codeUrl: String,
    @Json(name ="announcement")
    val announcement: String,
    @Json(name ="created_at")
    val createdAt: String,
    @Json(name ="participants")
    val participants: List<ParticipantRequest>,
    @Json(name ="participant_sessions")
    val participantSessions: List<UserSession>?,
    @Json(name ="circles")
    val circles: List<CircleConversation>?,
    @Json(name ="mute_until")
    val muteUntil: String
)
