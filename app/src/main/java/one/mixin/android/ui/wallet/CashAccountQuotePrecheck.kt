package one.mixin.android.ui.wallet

import java.math.BigDecimal

internal enum class CashAccountQuotePrecheckError {
    BELOW_MINIMUM_RECEIVE,
    UNSUPPORTED_TOKEN,
}

internal fun cashAccountQuotePrecheckError(
    amount: String,
    priceUsd: String?,
    minimumAmount: String,
): CashAccountQuotePrecheckError? {
    val priceValue = priceUsd?.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO }
        ?: return CashAccountQuotePrecheckError.UNSUPPORTED_TOKEN
    val amountValue = amount.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO } ?: return null
    val minimumValue = minimumAmount.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO } ?: return null
    val estimatedValue = amountValue.multiply(priceValue)
    return if (estimatedValue < minimumValue) {
        CashAccountQuotePrecheckError.BELOW_MINIMUM_RECEIVE
    } else {
        null
    }
}
