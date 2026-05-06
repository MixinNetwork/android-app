package one.mixin.android.ui.home.web3.trade.perps

import one.mixin.android.api.response.perps.PerpsMarket
import java.math.BigDecimal
import java.math.RoundingMode

private val perpsMinDisplayValue = BigDecimal("0.01")
const val PERPS_USD_SYMBOL = "\$"

fun PerpsMarket.changePercent(): BigDecimal {
    return try {
        BigDecimal(change).multiply(BigDecimal(100))
    } catch (e: Exception) {
        BigDecimal.ZERO
    }
}

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

fun formatPerpsUsdDecimal(value: BigDecimal?): String = formatPerpsFiatDecimal(value, PERPS_USD_SYMBOL)

fun formatPerpsSignedUsdDecimal(value: BigDecimal?): String = formatPerpsSignedFiatDecimal(value, PERPS_USD_SYMBOL)

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

fun formatPerpsSignedPercent(value: BigDecimal, withSign: Boolean = true): String {
    val percentText = "${formatPerpsPercentDecimal(value)}%"
    if (!withSign) {
        return percentText
    }

    return when {
        value > BigDecimal.ZERO -> "+$percentText"
        value < BigDecimal.ZERO -> "-$percentText"
        else -> percentText
    }
}

fun formatPerpsSignedPercent(value: Double, withSign: Boolean = true): String {
    return formatPerpsSignedPercent(BigDecimal.valueOf(value), withSign)
}

private fun formatPerpsPercentDecimal(value: BigDecimal): String {
    val safeValue = value.abs()
    val scale = if (safeValue > BigDecimal.ZERO && safeValue < BigDecimal("0.01")) 3 else 2
    return safeValue
        .setScale(scale, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
}
