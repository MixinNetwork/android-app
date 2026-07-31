package one.mixin.android.ui.wallet

internal fun normalizeGaslessPendingFeeAmount(feeAmount: String?): String =
    feeAmount?.toBigDecimalOrNull()?.stripTrailingZeros()?.toPlainString() ?: feeAmount.orEmpty()
