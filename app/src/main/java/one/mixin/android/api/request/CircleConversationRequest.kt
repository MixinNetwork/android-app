package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

data class CircleConversationRequest(
    @SerializedName("conversation_id")
    val conversationId: String,
    @SerializedName("contact_id")
    val contactId: String? = null
)
