package one.mixin.android.api.request

data class VerificationRequest(val phone: String?, val invitation: String?, val purpose: String)

enum class VerificationPurpose {
    SESSION,
    PHONE
}
