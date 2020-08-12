package one.mixin.android.api.response

import one.mixin.android.vo.Address
import one.mixin.android.vo.Asset
import one.mixin.android.vo.Snapshot
import one.mixin.android.vo.User

data class PaymentResponse(
    val recipient: User,
    val asset: Asset,
    val address: Address,
    val snapshot: Snapshot?,
    val amount: String,
    val status: String
)

enum class PaymentStatus {
    pending, paid
}
