package one.mixin.android.ui.home.web3.trade.perps

import java.math.BigDecimal
import java.math.RoundingMode

fun formatPerpsDisplayDecimal(value: BigDecimal?): String {
    val safeValue = value ?: BigDecimal.ZERO
    val absValue = safeValue.abs()
    if (absValue > BigDecimal.ZERO && absValue < BigDecimal("0.01")) {
        return "<0.01"
    }
    return safeValue.setScale(2, RoundingMode.HALF_UP).toPlainString()
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
    return safeValue.setScale(scale, RoundingMode.HALF_UP).toPlainString()
}
