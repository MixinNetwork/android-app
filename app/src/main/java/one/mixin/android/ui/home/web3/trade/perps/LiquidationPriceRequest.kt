package one.mixin.android.ui.home.web3.trade.perps

import com.google.gson.JsonElement
import kotlinx.coroutines.delay
import one.mixin.android.util.ErrorHandler
import java.math.BigDecimal

internal data class LiquidationPriceLimit(
    val maxAmount: String?,
    val maxLeverage: Int?,
)

internal sealed interface LiquidationPriceResult {
    data class Success(val price: String) : LiquidationPriceResult

    data class LimitExceeded(val limit: LiquidationPriceLimit) : LiquidationPriceResult

    data object Retry : LiquidationPriceResult

    data object Failure : LiquidationPriceResult
}

internal fun liquidationPriceResult(
    price: String?,
    errorCode: Int?,
    limit: LiquidationPriceLimit = LiquidationPriceLimit(null, null),
): LiquidationPriceResult {
    val validPrice = price?.takeIf { it.isNotBlank() }
    return when {
        validPrice != null -> LiquidationPriceResult.Success(validPrice)
        errorCode == ErrorHandler.PERPS_POSITION_SIZE_EXCEEDS_LEVERAGE_LIMIT -> {
            LiquidationPriceResult.LimitExceeded(limit)
        }
        errorCode == 500 -> LiquidationPriceResult.Retry
        else -> LiquidationPriceResult.Failure
    }
}

internal fun parseLiquidationPriceLimit(extra: JsonElement?): LiquidationPriceLimit {
    val extraObject = extra?.takeIf { it.isJsonObject }?.asJsonObject
    val maxAmount = extraObject
        ?.get("max_amount")
        ?.takeIf { !it.isJsonNull }
        ?.runCatching { asString }
        ?.getOrNull()
        ?.takeIf { it.isNotBlank() }
    val maxLeverage = extraObject
        ?.get("max_leverage")
        ?.takeIf { !it.isJsonNull }
        ?.runCatching { asString.toIntOrNull() }
        ?.getOrNull()
        ?.takeIf { it > 0 }
    return LiquidationPriceLimit(maxAmount, maxLeverage)
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
    onLimitExceeded: (LiquidationPriceLimit) -> Unit = {},
    request: suspend () -> LiquidationPriceResult,
): String? {
    while (true) {
        when (val result = request()) {
            LiquidationPriceResult.Failure -> return null
            is LiquidationPriceResult.LimitExceeded -> {
                onLimitExceeded(result.limit)
                return null
            }
            LiquidationPriceResult.Retry -> delay(retryDelayMillis)
            is LiquidationPriceResult.Success -> return result.price
        }
    }
}
