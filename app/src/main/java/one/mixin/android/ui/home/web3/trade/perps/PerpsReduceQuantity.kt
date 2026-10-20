package one.mixin.android.ui.home.web3.trade.perps

import java.math.BigDecimal
import java.math.RoundingMode

internal fun perpsReduceQuantity(input: String?, currentQuantity: String): BigDecimal? {
    val current = currentQuantity.toBigDecimalOrNull()?.abs() ?: return null
    if (input == null) return current.takeIf { it > BigDecimal.ZERO }
    if (input.length > 40 || input.any { it !in '0'..'9' && it != '.' }) return null
    val quantity = input.toBigDecimalOrNull() ?: return null
    return quantity.takeIf { it > BigDecimal.ZERO && it <= current }
}

internal fun perpsReductionQuantity(currentQuantity: String, markPrice: String?, input: String, isPercentage: Boolean, quantityScale: Int): BigDecimal? {
    if (quantityScale < 0) return null
    val current = currentQuantity.toBigDecimalOrNull()?.abs()?.takeIf { it > BigDecimal.ZERO } ?: return null
    if (isPercentage) {
        if ('.' in input) return null
    } else if (input.substringAfter('.', "").length > 2) {
        return null
    }
    val value = marginAdjustmentAmount(input) ?: return null
    val quantity = if (isPercentage) {
        if (value > BigDecimal(100)) return null
        if (value.compareTo(BigDecimal(100)) == 0) return current
        current.multiply(value).movePointLeft(2).setScale(quantityScale, RoundingMode.DOWN)
    } else {
        val price = markPrice?.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO } ?: return null
        if (value > current.multiply(price)) return null
        value.divide(price, quantityScale, RoundingMode.DOWN)
    }
    return quantity.takeIf { it >= BigDecimal.ZERO && it <= current }
}

internal fun perpsReductionValue(value: String?, quantity: String, currentQuantity: String): BigDecimal? {
    val total = value?.toBigDecimalOrNull() ?: return null
    val reduction = perpsReduceQuantity(quantity, currentQuantity) ?: return null
    return total.multiply(reduction).divide(BigDecimal(currentQuantity).abs(), 16, RoundingMode.DOWN)
}

internal fun perpsReductionMeetsMinimum(quantity: BigDecimal, current: BigDecimal, price: BigDecimal?, minimum: BigDecimal): Boolean {
    if (quantity <= BigDecimal.ZERO) return false
    val position = current.abs()
    if (quantity.compareTo(position) == 0 || minimum <= BigDecimal.ZERO) return true
    if (price == null || price <= BigDecimal.ZERO) return false
    return quantity.multiply(price) >= minimum
}

internal fun perpsReductionMinimumAmount(price: BigDecimal?, minimum: BigDecimal, quantityScale: Int): BigDecimal? {
    if (price == null || price <= BigDecimal.ZERO || quantityScale < 0) return null
    val step = BigDecimal.ONE.movePointLeft(quantityScale)
    val quantity = minimum.divide(price, quantityScale, RoundingMode.CEILING).max(step)
    return quantity.multiply(price).setScale(2, RoundingMode.CEILING)
}

internal fun perpsReductionInputPercentage(amount: BigDecimal, size: BigDecimal?): BigDecimal? =
    size?.takeIf { it > BigDecimal.ZERO }?.let {
        amount.multiply(BigDecimal(100)).divide(it, 0, RoundingMode.DOWN)
    }
