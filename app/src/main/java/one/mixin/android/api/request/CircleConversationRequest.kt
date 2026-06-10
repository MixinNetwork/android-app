package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

data class CircleConversationPayload(
    val conversationId: String,
    val userId: String? = null,
)

data class CircleConversationRequest(
    @SerializedName("conversation_id")
    val conversationId: String,
    @SerializedName("user_id")
    val userId: String? = null,
    @SerializedName("action")
    val action: String,
)
