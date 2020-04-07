package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

data class CircleConversationRequest(
    @SerializedName("conversation_id")
    val conversationId: String,
    @SerializedName("user_id")
    val userId: String? = null
)
