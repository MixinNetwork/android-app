package one.mixin.android.api.response

data class PaymentResponse(
    val status: String
)

enum class PaymentStatus {
    pending, paid
}
