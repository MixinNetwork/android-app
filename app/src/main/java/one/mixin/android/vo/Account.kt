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
    val avatar_url: String?,
    var phone: String,
    val avatar_base64: String?,
    val invitation_code: String,
    var pin_token: String,
    val consumed_count: Int,
    val code_id: String,
    val code_url: String,
    val created_at: String,
    val receive_message_source: String,
    @SerializedName("has_pin")
    val hasPin: Boolean,
    val accept_conversation_source: String,
    @SerializedName("has_emergency_contact")
    var hasEmergencyContact: Boolean
)

fun Account.toUser(): User =
    User(userId, identity_number, relationship, full_name, avatar_url, phone, null, created_at, null, hasPin)
