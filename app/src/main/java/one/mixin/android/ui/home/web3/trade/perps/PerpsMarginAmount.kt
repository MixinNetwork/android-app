package one.mixin.android.ui.home.web3.trade.perps

import com.google.gson.JsonElement
import one.mixin.android.extension.numberFormat8
import one.mixin.android.ui.home.web3.trade.TRADE_INPUT_MAX_DECIMAL_PLACES
import one.mixin.android.vo.safe.TokenItem
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.roundToInt

internal fun perpsMarginTokenBalance(token: TokenItem?): BigDecimal? = token?.balance?.toBigDecimalOrNull()

internal fun formatPerpsMarginAmount(value: BigDecimal?): String =
    if (value == null || value.setScale(2, RoundingMode.HALF_UP).signum() == 0) "0" else formatPerpsPrice(value, 2)

internal fun formatPerpsMarginLimit(currentMargin: String?, availableMargin: BigDecimal, isPercentage: Boolean): String {
    if (isPercentage) {
        val percentage = marginReductionPercentage(currentMargin, availableMargin) ?: BigDecimal.ZERO
        return "${percentage.setScale(2, RoundingMode.DOWN).numberFormat8()}%"
    }
    val amount = availableMargin.setScale(TRADE_INPUT_MAX_DECIMAL_PLACES, RoundingMode.DOWN)
    return if (amount.signum() == 0) "0" else "$PERPS_USD_SYMBOL${amount.numberFormat8()}"
}

internal fun marginLiquidationLossPercent(entryPrice: String, liquidationPrice: String?): BigDecimal? {
    val entry = entryPrice.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO } ?: return null
    val liquidation = liquidationPrice?.toBigDecimalOrNull()?.takeIf { it >= BigDecimal.ZERO } ?: return null
    return (entry - liquidation).abs().multiply(BigDecimal(100)).divide(entry, 2, RoundingMode.HALF_UP)
}

internal fun snapMarginReductionPercentage(value: Float): Int {
    val percentage = value.coerceIn(0f, 100f).roundToInt()
    val marker = (percentage / 25f).roundToInt() * 25
    return if (abs(percentage - marker) <= 3) marker else percentage
}

internal fun formatMarginAdjustmentInput(value: BigDecimal, isPercentage: Boolean): String =
    value.setScale(if (isPercentage) 0 else 2, RoundingMode.DOWN).toPlainString()

internal fun marginAdjustmentAmount(amount: String): BigDecimal? {
    if (amount.length > 40 || amount.any { it !in '0'..'9' && it != '.' } || amount.count { it == '.' } > 1 ||
        amount.substringAfter('.', "").length > TRADE_INPUT_MAX_DECIMAL_PLACES) return null
    return amount.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO }
}

internal fun marginAfterAdjustment(currentMargin: String?, amount: String, increase: Boolean, availableMargin: BigDecimal? = null): BigDecimal? {
    val current = currentMargin?.toBigDecimalOrNull()?.takeIf { it >= BigDecimal.ZERO } ?: return null
    val adjustment = marginAdjustmentAmount(amount) ?: return null
    if (!increase && availableMargin != null && adjustment > availableMargin) return null
    return (if (increase) current + adjustment else current - adjustment).takeIf { it >= BigDecimal.ZERO }
}

internal fun availableMarginFromError(extra: JsonElement?): BigDecimal? {
    val value = extra?.takeIf { it.isJsonObject }?.asJsonObject?.get("available_margin")
        ?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive ?: return null
    if (!value.isString && !value.isNumber) return null
    return value.asString.toBigDecimalOrNull()?.takeIf { it >= BigDecimal.ZERO }
}

internal fun reduceMarginAmount(currentMargin: String?, input: String, isPercentage: Boolean): BigDecimal? {
    if (isPercentage && '.' in input) return null
    val value = marginAdjustmentAmount(input) ?: return null
    if (!isPercentage) return value
    val margin = currentMargin?.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO } ?: return null
    return margin.multiply(value).movePointLeft(2)
        .setScale(TRADE_INPUT_MAX_DECIMAL_PLACES, RoundingMode.DOWN)
        .takeIf { it > BigDecimal.ZERO }
}

internal fun marginReductionPercentage(currentMargin: String?, amount: BigDecimal?): BigDecimal? {
    val margin = currentMargin?.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO } ?: return null
    return amount?.multiply(BigDecimal(100))?.divide(margin, TRADE_INPUT_MAX_DECIMAL_PLACES, RoundingMode.DOWN)
}
