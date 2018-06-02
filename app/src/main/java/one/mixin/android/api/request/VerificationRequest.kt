package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

data class VerificationRequest(
    val phone: String?,
    val invitation: String?,
    val purpose: String,
    @SerializedName("g_recaptcha_response")
    val gRecaptchaResponse: String?)

enum class VerificationPurpose {
    SESSION,
    PHONE
}
