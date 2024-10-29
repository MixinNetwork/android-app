package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName
import one.mixin.android.BuildConfig

data class VerificationRequest(
    val phone: String? = null,
    val purpose: String,
    @SerializedName("g_recaptcha_response")
    var gRecaptchaResponse: String? = null,
    @SerializedName("hcaptcha_response")
    var hCaptchaResponse: String? = null,
    val package_name: String = BuildConfig.APPLICATION_ID,
    val public_key_hex: String? = null,
    val message_hex: String? = null,
    val signature_hex: String? = null
)

enum class VerificationPurpose {
    SESSION,
    PHONE,
    DEACTIVATED,
    ANONYMOUS_SESSION
}
