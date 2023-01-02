package one.mixin.android.pay

import java.math.BigDecimal
import java.util.UUID

data class ExternalTransfer(
    val destination: String,
    val amount: BigDecimal,
    val assetId: String,
    val fee: BigDecimal?,
    val memo: String? = null,
)

fun generateAddressId(userId: String, assetId: String, destination: String, tag: String?): String {
    return UUID.nameUUIDFromBytes((userId + assetId + destination + (tag ?: "")).toByteArray()).toString()
}
