package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName
import one.mixin.android.BuildConfig

data class VerificationRequest(
    val phone: String?,
    val purpose: String,
    @SerializedName("g_recaptcha_response")
    val gRecaptchaResponse: String?,
    val package_name: String = BuildConfig.APPLICATION_ID
)

enum class VerificationPurpose {
    SESSION,
    PHONE
}
