package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

class ConversationRequest(
    @SerializedName("conversation_id")
    val conversationId: String,
    @SerializedName("category")
    val category: String? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("icon_base64")
    val iconBase64: String? = null,
    @SerializedName("announcement")
    val announcement: String? = null,
    @SerializedName("participants")
    val participants: List<ParticipantRequest>? = null,
    @SerializedName("duration")
    val duration: Long? = null,
)
