package one.mixin.android.ui.home.web3.trade.perps

import java.math.BigDecimal
import java.math.RoundingMode

private val perpsMinDisplayValue = BigDecimal("0.01")

fun formatPerpsDisplayDecimal(value: BigDecimal?): String {
    val safeValue = value ?: BigDecimal.ZERO
    val absValue = safeValue.abs()
    if (absValue > BigDecimal.ZERO && absValue < perpsMinDisplayValue) {
        return "<0.01"
    }
    return safeValue.setScale(2, RoundingMode.HALF_UP).toPlainString()
}

fun formatPerpsFiatDecimal(value: BigDecimal?, fiatSymbol: String): String {
    val safeValue = value ?: BigDecimal.ZERO
    val absValue = safeValue.abs()
    return if (absValue > BigDecimal.ZERO && absValue < perpsMinDisplayValue) {
        "<${fiatSymbol}0.01"
    } else {
        "$fiatSymbol${absValue.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()}"
    }
}

fun formatPerpsSignedFiatDecimal(value: BigDecimal?, fiatSymbol: String): String {
    val safeValue = value ?: BigDecimal.ZERO
    return when {
        safeValue > BigDecimal.ZERO -> "+${formatPerpsFiatDecimal(safeValue, fiatSymbol)}"
        safeValue < BigDecimal.ZERO -> "-${formatPerpsFiatDecimal(safeValue.abs(), fiatSymbol)}"
        else -> formatPerpsFiatDecimal(BigDecimal.ZERO, fiatSymbol)
    }
}

fun calculateClosedRoe(
    entryPrice: String?,
    closePrice: String?,
    side: String,
    leverage: Int,
): BigDecimal {
    val entry = entryPrice?.toBigDecimalOrNull() ?: return BigDecimal.ZERO
    val close = closePrice?.toBigDecimalOrNull() ?: return BigDecimal.ZERO
    if (entry <= BigDecimal.ZERO || leverage <= 0) {
        return BigDecimal.ZERO
    }

    val direction = if (side.equals("short", ignoreCase = true)) BigDecimal(-1) else BigDecimal.ONE
    return close
        .subtract(entry)
        .divide(entry, 8, RoundingMode.HALF_UP)
        .multiply(BigDecimal(leverage))
        .multiply(BigDecimal(100))
        .multiply(direction)
}

fun formatPerpsSignedPercent(value: BigDecimal): String {
    val sign = when {
        value > BigDecimal.ZERO -> "+"
        value < BigDecimal.ZERO -> "-"
        else -> ""
    }
    return "$sign${formatPerpsPercentDecimal(value.abs())}%"
}

fun formatPerpsSignedPercent(value: Double): String {
    return formatPerpsSignedPercent(BigDecimal.valueOf(value))
}

private fun formatPerpsPercentDecimal(value: BigDecimal): String {
    val safeValue = value.abs()
    val scale = if (safeValue > BigDecimal.ZERO && safeValue < BigDecimal("0.01")) 3 else 2
    return safeValue
        .setScale(scale, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
}
