package one.mixin.android.pay

import java.math.BigDecimal

data class ExternalTransfer(
    val destination: String,
    val amount: BigDecimal,
    val assetId: String,
    val fee: BigDecimal?,
)
