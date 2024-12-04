package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

data class VerificationResponse(
    val type: String,
    val id: String,
    @SerializedName("has_emergency_contact")
    val hasEmergencyContact: Boolean = false,
    @SerializedName("contact_id")
    val contactId: String? = null,
    @SerializedName("deactivation_requested_at")
    val deactivationRequestedAt: String?,
    @SerializedName("deactivation_effective_at")
    val deactivationEffectiveAt: String?,
)
