package one.mixin.android.vo

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
open class Account(
    @SerializedName("user_id")
    @Json(name = "user_id")
    val userId: String,
    @SerializedName("session_id")
    @Json(name = "session_id")
    val sessionId: String,
    val type: String,
    @SerializedName("identity_number")
    @Json(name = "identity_number")
    val identityNumber: String,
    /**
     * @see UserRelationship
     */
    val relationship: String,
    @SerializedName("full_name")
    @Json(name = "full_name")
    val fullName: String?,
    var biography: String?,
    @SerializedName("avatar_url")
    @Json(name = "avatar_url")
    val avatarUrl: String?,
    var phone: String,
    @SerializedName("avatar_base64")
    @Json(name = "avatar_base64")
    val avatarBase64: String?,
    @SerializedName("pin_token")
    @Json(name = "pin_token")
    var pinToken: String,
    @SerializedName("code_id")
    @Json(name = "code_id")
    val codeId: String,
    @SerializedName("code_url")
    @Json(name = "code_url")
    val codeUrl: String,
    @SerializedName("created_at")
    @Json(name = "created_at")
    val createdAt: String,
    @SerializedName("receive_message_source")
    @Json(name = "receive_message_source")
    val receiveMessageSource: String,
    @SerializedName("has_pin")
    @Json(name = "has_pin")
    val hasPin: Boolean,
    @SerializedName("accept_conversation_source")
    @Json(name = "accept_conversation_source")
    val acceptConversationSource: String,
    @SerializedName("accept_search_source")
    @Json(name = "accept_search_source")
    val acceptSearchSource: String,
    @SerializedName("has_emergency_contact")
    @Json(name = "has_emergency_contact")
    var hasEmergencyContact: Boolean,
    @SerializedName("fiat_currency")
    @Json(name = "fiat_currency")
    var fiatCurrency: String,
    @SerializedName("transfer_notification_threshold")
    @Json(name = "transfer_notification_threshold")
    val transferNotificationThreshold: Double = 0.0,
    @SerializedName("transfer_confirmation_threshold")
    @Json(name = "transfer_confirmation_threshold")
    val transferConfirmationThreshold: Double = 100.0
)

fun Account.toUser(): User {
    return User(userId, identityNumber, relationship, biography ?: "", fullName, avatarUrl, phone, null, createdAt, null, hasPin)
}
