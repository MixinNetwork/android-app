package one.mixin.android.ui.web

data class GoogleWalletProvisioningRequest(
    val requestId: String,
    val opaquePaymentCard: String,
    val displayName: String,
    val lastDigits: String,
) {
    fun isValid(): Boolean =
        requestId.isNotBlank() &&
            requestId.length <= 128 &&
            opaquePaymentCard.isNotBlank() &&
            opaquePaymentCard.length <= 65536 &&
            displayName.isNotBlank() &&
            displayName.length <= 64 &&
            lastDigits.length in 1..4 &&
            lastDigits.all(Char::isDigit)
}

enum class GoogleWalletProvisioningResult { Success, Cancelled, Error }
