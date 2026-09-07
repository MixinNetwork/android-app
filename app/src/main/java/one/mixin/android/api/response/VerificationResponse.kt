package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

data class VerificationResponse(
    @SerializedName("type")
    val type: String,
    @SerializedName("id")
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
