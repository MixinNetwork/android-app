package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName
import one.mixin.android.api.request.ParticipantRequest
import one.mixin.android.vo.CircleConversation

open class ConversationResponse(
    @SerializedName("conversation_id")
    val conversationId: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("category")
    val category: String,
    @SerializedName("creator_id")
    val creatorId: String,
    @SerializedName("icon_url")
    val iconUrl: String,
    @SerializedName("code_url")
    val codeUrl: String,
    @SerializedName("announcement")
    val announcement: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("participants")
    val participants: List<ParticipantRequest>,
    @SerializedName("participant_sessions")
    val participantSessions: List<UserSession>?,
    @SerializedName("circles")
    val circles: List<CircleConversation>?,
    @SerializedName("mute_until")
    val muteUntil: String,
    @SerializedName("expire_in")
    val expireIn: Long?
)
