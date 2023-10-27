package one.mixin.android.vo

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

open class Account(
    @Expose
    @SerializedName("user_id")
    val userId: String,
    @Expose
    @SerializedName("session_id")
    val sessionId: String,
    @Expose
    val type: String,
    @Expose
    @SerializedName("identity_number")
    val identityNumber: String,
    /**
     * @see UserRelationship
     */
    @Expose
    val relationship: String,
    @Expose
    @SerializedName("full_name")
    val fullName: String?,
    @Expose
    var biography: String?,
    @Expose
    @SerializedName("avatar_url")
    val avatarUrl: String?,
    @Expose
    var phone: String,
    @Expose
    @SerializedName("avatar_base64")
    val avatarBase64: String?,
    @Expose
    @SerializedName("pin_token")
    var pinToken: String,
    @Expose
    @SerializedName("code_id")
    val codeId: String,
    @Expose
    @SerializedName("code_url")
    val codeUrl: String,
    @Expose
    @SerializedName("created_at")
    val createdAt: String,
    @Expose
    @SerializedName("receive_message_source")
    val receiveMessageSource: String,
    @Expose
    @SerializedName("has_pin")
    val hasPin: Boolean,
    @Expose
    @SerializedName("tip_key_base64")
    val tipKeyBase64: String,
    @Expose
    @SerializedName("tip_counter")
    val tipCounter: Int,
    @Expose
    @SerializedName("accept_conversation_source")
    val acceptConversationSource: String,
    @Expose
    @SerializedName("accept_search_source")
    val acceptSearchSource: String,
    @Expose
    @SerializedName("has_emergency_contact")
    var hasEmergencyContact: Boolean,
    @Expose
    @SerializedName("has_safe")
    var hasSafe: Boolean,
    @Expose
    @SerializedName("fiat_currency")
    var fiatCurrency: String,
    @Expose
    @SerializedName("transfer_notification_threshold")
    val transferNotificationThreshold: Double = 0.0,
    @Expose
    @SerializedName("transfer_confirmation_threshold")
    val transferConfirmationThreshold: Double = 100.0,
    @Expose
    @SerializedName("features")
    val features: ArrayList<String>? = null,

    @Expose(serialize = false, deserialize = true)
    @SerializedName("salt")
    var salt: String?,
)

fun Account.toUser(): User {
    return User(userId, identityNumber, relationship, biography ?: "", fullName, avatarUrl, phone, null, createdAt, null, hasPin)
}
