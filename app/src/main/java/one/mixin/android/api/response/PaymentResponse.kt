package one.mixin.android.api.response

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PaymentResponse(
    val status: String
)

enum class PaymentStatus {
    pending, paid
}
