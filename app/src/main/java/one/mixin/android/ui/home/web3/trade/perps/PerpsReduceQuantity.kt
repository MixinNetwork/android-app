package one.mixin.android.ui.home.web3.trade.perps

import java.math.BigDecimal
import java.math.RoundingMode

internal fun perpsReduceQuantity(input: String, currentQuantity: String): BigDecimal? {
    if (input.length > 40 || input.any { it !in '0'..'9' && it != '.' }) return null
    val quantity = input.toBigDecimalOrNull() ?: return null
    val current = currentQuantity.toBigDecimalOrNull()?.abs() ?: return null
    return quantity.takeIf { it > BigDecimal.ZERO && it <= current }
}

internal fun perpsReductionQuantity(currentQuantity: String, markPrice: String?, input: String, isPercentage: Boolean, quantityScale: Int): BigDecimal? {
    if (quantityScale < 0) return null
    val current = currentQuantity.toBigDecimalOrNull()?.abs()?.takeIf { it > BigDecimal.ZERO } ?: return null
    val value = marginAdjustmentAmount(input) ?: return null
    val quantity = if (isPercentage) {
        if ('.' in input || value > BigDecimal(100)) return null
        current.multiply(value).movePointLeft(2).setScale(quantityScale, RoundingMode.DOWN)
    } else {
        val price = markPrice?.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO } ?: return null
        if (value > current.multiply(price)) return null
        value.divide(price, quantityScale, RoundingMode.DOWN)
    }
    return perpsReduceQuantity(quantity.toPlainString(), currentQuantity)
}

internal fun perpsReductionValue(value: String?, quantity: String, currentQuantity: String): BigDecimal? {
    val total = value?.toBigDecimalOrNull() ?: return null
    val reduction = perpsReduceQuantity(quantity, currentQuantity) ?: return null
    return total.multiply(reduction).divide(BigDecimal(currentQuantity).abs(), 16, RoundingMode.DOWN)
}
