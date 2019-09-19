package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

data class AccountUpdateRequest(
    @SerializedName("full_name")
    val fullName: String? = null,
    @SerializedName("avatar_base64")
    val avatarBase64: String? = null,
    @SerializedName("receive_message_source")
    val receiveMessageSource: String? = null,
    @SerializedName("accept_conversation_source")
    val acceptConversationSource: String? = null,
    @SerializedName("biography")
    val biography: String? = null
)
