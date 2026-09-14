package one.mixin.android.ui.home.web3.trade.perps

import com.google.gson.JsonElement
import one.mixin.android.ui.home.web3.trade.TRADE_INPUT_MAX_DECIMAL_PLACES
import java.math.BigDecimal
import java.math.RoundingMode

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
