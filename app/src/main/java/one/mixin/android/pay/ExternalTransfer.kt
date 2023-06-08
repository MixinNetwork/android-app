package one.mixin.android.pay

import ulid.ULID
import java.math.BigDecimal

data class ExternalTransfer(
    val destination: String,
    val amount: String,
    val assetId: String,
    val fee: BigDecimal?,
    val memo: String? = null,
)

fun generateAddressId(userId: String, assetId: String, destination: String, tag: String?): String {
    return ULID.fromBytes((userId + assetId + destination + (tag ?: "")).toByteArray()).toString()
}
