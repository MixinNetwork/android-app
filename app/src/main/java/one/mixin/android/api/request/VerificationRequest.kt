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
    @SerializedName("package_name")
    val packageName: String = BuildConfig.APPLICATION_ID,
    @SerializedName("master_public_hex")
    val masterPublicHex: String? = null,
    @SerializedName("master_message_hex")
    val masterMessageHex: String? = null,
    @SerializedName("master_signature_hex")
    val masterSignatureHex: String? = null,
)

enum class VerificationPurpose {
    SESSION,
    PHONE,
    DEACTIVATED,
    ANONYMOUS_SESSION
}
