package one.mixin.android.vo

import com.google.gson.annotations.SerializedName

open class Account(
    @SerializedName("user_id")
    val userId: String,
    val session_id: String,
    val type: String,
    val identity_number: String,
    /**
     * @see UserRelationship
     */
    val relationship: String,
    val full_name: String?,
    var biography: String?,
    val avatar_url: String?,
    var phone: String,
    val avatar_base64: String?,
    var pin_token: String,
    val code_id: String,
    val code_url: String,
    val created_at: String,
    val receive_message_source: String,
    @SerializedName("has_pin")
    val hasPin: Boolean,
    val accept_conversation_source: String,
    @SerializedName("accept_search_source")
    val acceptSearchSource: String,
    @SerializedName("has_emergency_contact")
    var hasEmergencyContact: Boolean,
    @SerializedName("fiat_currency")
    var fiatCurrency: String,
    @SerializedName("transfer_notification_threshold")
    val transferNotificationThreshold: Double = 0.0,
    @SerializedName("transfer_confirmation_threshold")
    val transferConfirmationThreshold: Double = 100.0
)

fun Account.toUser(): User {
    if (biography == null) {
        biography = ""
    }
    return User(userId, identity_number, relationship, biography!!, full_name, avatar_url, phone, null, created_at, null, hasPin)
}
