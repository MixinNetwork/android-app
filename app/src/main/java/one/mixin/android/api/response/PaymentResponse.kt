package one.mixin.android.api.response

import one.mixin.android.vo.Asset
import one.mixin.android.vo.User

data class PaymentResponse(
    val recipient: User,
    val asset: Asset,
    val amount: String,
    val status: String
)

enum class PaymentStatus {
    pending, paid
}
