package one.mixin.android.api.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import one.mixin.android.BuildConfig

@JsonClass(generateAdapter = true)
data class VerificationRequest(
    val phone: String?,
    val purpose: String,
    @Json(name ="g_recaptcha_response")
    var gRecaptchaResponse: String? = null,
    @Json(name ="hcaptcha_response")
    var hCaptchaResponse: String? = null,
    val package_name: String = BuildConfig.APPLICATION_ID
)

enum class VerificationPurpose {
    SESSION,
    PHONE
}
