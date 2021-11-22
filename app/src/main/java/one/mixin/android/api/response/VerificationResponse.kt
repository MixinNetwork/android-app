package one.mixin.android.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VerificationResponse(
    val type: String,
    val id: String,
    @Json(name ="has_emergency_contact")
    val hasEmergencyContact: Boolean = false,
    @Json(name ="contact_id")
    val contactId: String? = null
)
