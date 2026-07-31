package one.mixin.android.ui.home.web3.trade.perps

import kotlinx.coroutines.delay
import java.math.BigDecimal

internal sealed interface LiquidationPriceResult {
    data class Success(val price: String) : LiquidationPriceResult

    data object Retry : LiquidationPriceResult

    data object Failure : LiquidationPriceResult
}

internal fun liquidationPriceResult(
    price: String?,
    errorCode: Int?,
): LiquidationPriceResult {
    val validPrice = price?.takeIf { it.isNotBlank() }
    return when {
        validPrice != null -> LiquidationPriceResult.Success(validPrice)
        errorCode == 500 -> LiquidationPriceResult.Retry
        else -> LiquidationPriceResult.Failure
    }
}

internal fun shouldRequestLiquidationPrice(
    amount: BigDecimal?,
    minimumAmount: BigDecimal,
): Boolean {
    return amount != null &&
        amount > BigDecimal.ZERO &&
        (minimumAmount <= BigDecimal.ZERO || amount >= minimumAmount)
}

internal suspend fun requestLiquidationPrice(
    retryDelayMillis: Long = 1000L,
    request: suspend () -> LiquidationPriceResult,
): String? {
    while (true) {
        when (val result = request()) {
            LiquidationPriceResult.Failure -> return null
            LiquidationPriceResult.Retry -> delay(retryDelayMillis)
            is LiquidationPriceResult.Success -> return result.price
        }
    }
}
