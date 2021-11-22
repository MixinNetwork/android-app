package one.mixin.android.vo

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
open class Account(
    @Json(name ="user_id")
    val userId: String,
    @Json(name ="session_id")
    val sessionId: String,
    val type: String,
    @Json(name ="identity_number")
    val identityNumber: String,
    /**
     * @see UserRelationship
     */
    val relationship: String,
    @Json(name ="full_name")
    val fullName: String?,
    var biography: String?,
    @Json(name ="avatar_url")
    val avatarUrl: String?,
    var phone: String,
    @Json(name ="avatar_base64")
    val avatarBase64: String?,
    @Json(name ="pin_token")
    var pinToken: String,
    @Json(name ="code_id")
    val codeId: String,
    @Json(name ="code_url")
    val codeUrl: String,
    @Json(name ="created_at")
    val createdAt: String,
    @Json(name ="receive_message_source")
    val receiveMessageSource: String,
    @Json(name ="has_pin")
    val hasPin: Boolean,
    @Json(name ="accept_conversation_source")
    val acceptConversationSource: String,
    @Json(name ="accept_search_source")
    val acceptSearchSource: String,
    @Json(name ="has_emergency_contact")
    var hasEmergencyContact: Boolean,
    @Json(name ="fiat_currency")
    var fiatCurrency: String,
    @Json(name ="transfer_notification_threshold")
    val transferNotificationThreshold: Double = 0.0,
    @Json(name ="transfer_confirmation_threshold")
    val transferConfirmationThreshold: Double = 100.0
)

fun Account.toUser(): User {
    return User(userId, identityNumber, relationship, biography ?: "", fullName, avatarUrl, phone, null, createdAt, null, hasPin)
}
