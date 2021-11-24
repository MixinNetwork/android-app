package one.mixin.android.api.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AccountUpdateRequest(
    @Json(name = "full_name")
    val fullName: String? = null,
    @Json(name = "avatar_base64")
    val avatarBase64: String? = null,
    @Json(name = "receive_message_source")
    val receiveMessageSource: String? = null,
    @Json(name = "accept_conversation_source")
    val acceptConversationSource: String? = null,
    @Json(name = "accept_search_source")
    val acceptSearchSource: String? = null,
    @Json(name = "biography")
    val biography: String? = null,
    @Json(name = "fiat_currency")
    val fiatCurrency: String? = null,
    @Json(name = "transfer_notification_threshold")
    val transferNotificationThreshold: Double? = null,
    @Json(name = "transfer_confirmation_threshold")
    val transferConfirmationThreshold: Double? = null
)
