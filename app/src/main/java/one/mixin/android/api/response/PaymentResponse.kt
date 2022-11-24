@file:Suppress("ktlint:enum-entry-name-case", "EnumEntryName")

package one.mixin.android.api.response

data class PaymentResponse(
    val status: String
)

enum class PaymentStatus {
    pending, paid
}
