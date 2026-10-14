package one.mixin.android.ui.home.web3.trade.perps

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.gson.JsonElement
import kotlinx.coroutines.delay
import one.mixin.android.R
import one.mixin.android.util.ErrorHandler
import java.math.BigDecimal

internal class LiquidationPriceState {
    var price by mutableStateOf<String?>(null)
    var isLoading by mutableStateOf(false)
        private set

    suspend fun refresh(request: suspend () -> String?) {
        isLoading = price == null
        try {
            price = request()
        } finally {
            isLoading = false
        }
    }
}

internal data class LiquidationPriceLimit(
    val maxAmount: String?,
    val maxLeverage: Int?,
) {
    fun errorMessage(context: Context, symbol: String, isAddingPosition: Boolean = false): String =
        maxAmount?.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO }?.let {
            context.getString(
                if (isAddingPosition) R.string.error_perps_position_size_exceeds_leverage_limit_add else R.string.error_perps_position_size_exceeds_leverage_limit_value,
                "${it.stripTrailingZeros().toPlainString()} $symbol".trim(),
            )
        } ?: context.getString(
            if (isAddingPosition) R.string.error_perps_position_size_exceeds_leverage_limit_cannot_add else R.string.error_perps_position_size_exceeds_leverage_limit,
        )
}

internal sealed interface LiquidationPriceResult {
    data class Success(val price: String) : LiquidationPriceResult

    data class LimitExceeded(val limit: LiquidationPriceLimit) : LiquidationPriceResult

    data class MarginExceeded(val availableMargin: BigDecimal?, val message: String? = null) : LiquidationPriceResult

    data object Retry : LiquidationPriceResult

    data class Failure(val message: String?) : LiquidationPriceResult
}

internal fun liquidationPriceResult(
    price: String?,
    errorCode: Int?,
    limit: LiquidationPriceLimit = LiquidationPriceLimit(null, null),
    availableMargin: BigDecimal? = null,
    errorMessage: String? = null,
): LiquidationPriceResult {
    val validPrice = price?.takeIf { it.isNotBlank() }
    return when {
        validPrice != null -> LiquidationPriceResult.Success(validPrice)
        errorCode == 10653 -> LiquidationPriceResult.MarginExceeded(availableMargin, errorMessage)
        errorCode == ErrorHandler.PERPS_POSITION_SIZE_EXCEEDS_LEVERAGE_LIMIT -> {
            LiquidationPriceResult.LimitExceeded(limit)
        }
        errorCode == 500 -> LiquidationPriceResult.Retry
        else -> LiquidationPriceResult.Failure(errorMessage)
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
    onMarginExceeded: ((BigDecimal?) -> Unit)? = null,
    onFailure: (String?) -> Unit = {},
    request: suspend () -> LiquidationPriceResult,
): String? {
    while (true) {
        when (val result = request()) {
            is LiquidationPriceResult.Failure -> {
                onFailure(result.message)
                return null
            }
            is LiquidationPriceResult.LimitExceeded -> {
                onLimitExceeded(result.limit)
                return null
            }
            is LiquidationPriceResult.MarginExceeded -> {
                if (onMarginExceeded != null) onMarginExceeded(result.availableMargin) else onFailure(result.message)
                return null
            }
            LiquidationPriceResult.Retry -> delay(retryDelayMillis)
            is LiquidationPriceResult.Success -> return result.price
        }
    }
}
