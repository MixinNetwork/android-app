package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

data class VerificationResponse(
    val type: String,
    val id: String,
    @SerializedName("has_emergency_contact")
    val hasEmergencyContact: Boolean = false,
    @SerializedName("contact_id")
    val contactId: String? = null,
    @SerializedName("deactivated_at")
    val deactivatedAt: String?
)
