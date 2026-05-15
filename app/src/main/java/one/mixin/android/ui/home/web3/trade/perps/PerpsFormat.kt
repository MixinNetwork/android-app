package one.mixin.android.ui.home.web3.trade.perps

import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.extension.priceFormat
import java.text.DecimalFormat
import java.math.BigDecimal
import java.math.RoundingMode

const val PERPS_USD_SYMBOL = "\$"
const val DEFAULT_PERPS_PRICE_SCALE = 2

fun PerpsMarket.changePercent(): BigDecimal {
    return try {
        BigDecimal(change).multiply(BigDecimal(100))
    } catch (e: Exception) {
        BigDecimal.ZERO
    }
}

fun formatPerpsFiatDecimal(value: BigDecimal?, fiatSymbol: String): String {
    val safeValue = value ?: BigDecimal.ZERO
    val absValue = safeValue.abs()
    return "$fiatSymbol${absValue.setScale(2, RoundingMode.HALF_UP).priceFormat()}"
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

fun formatPerpsPrice(value: BigDecimal?, priceScale: Int = DEFAULT_PERPS_PRICE_SCALE): String {
    val safeValue = value ?: BigDecimal.ZERO
    val safeScale = priceScale.coerceAtLeast(0)
    val scaledValue = safeValue.setScale(safeScale, RoundingMode.HALF_UP)
    val pattern = if (safeScale == 0) {
        ",##0"
    } else {
        ",##0.${"0".repeat(safeScale)}"
    }
    return "$PERPS_USD_SYMBOL${DecimalFormat(pattern).format(scaledValue)}"
}

fun formatPerpsPrice(rawPrice: String?, priceScale: Int = DEFAULT_PERPS_PRICE_SCALE): String {
    return formatPerpsPrice(rawPrice?.toBigDecimalOrNull(), priceScale)
}

fun formatPerpsPriceInput(value: BigDecimal, priceScale: Int = DEFAULT_PERPS_PRICE_SCALE): String {
    return value
        .setScale(priceScale.coerceAtLeast(0), RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
}

fun formatPerpsRawUsdDecimal(value: BigDecimal?): String {
    val safeValue = value ?: BigDecimal.ZERO
    val absValue = safeValue.abs()
    return when {
        absValue.compareTo(BigDecimal.ZERO) == 0 -> "${PERPS_USD_SYMBOL}0.00"
        absValue < BigDecimal("0.01") -> "<${PERPS_USD_SYMBOL}0.01"
        else -> "$PERPS_USD_SYMBOL${absValue.setScale(2, RoundingMode.FLOOR).priceFormat()}"
    }
}

fun formatPerpsSignedRawUsdDecimal(value: BigDecimal?): String {
    val safeValue = value ?: BigDecimal.ZERO
    return when {
        safeValue > BigDecimal.ZERO -> "+${formatPerpsRawUsdDecimal(safeValue)}"
        safeValue < BigDecimal.ZERO -> "-${formatPerpsRawUsdDecimal(safeValue.abs())}"
        else -> formatPerpsRawUsdDecimal(BigDecimal.ZERO)
    }
}

fun formatPerpsQuantity(value: BigDecimal?): String {
    val safeValue = value ?: BigDecimal.ZERO
    return if (safeValue.compareTo(BigDecimal.ZERO) == 0) {
        "0"
    } else {
        safeValue.stripTrailingZeros().toPlainString()
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
    val scaled = safeValue.setScale(2, RoundingMode.FLOOR)
    if (scaled.compareTo(BigDecimal.ZERO) == 0) return "0.0"
    return scaled.stripTrailingZeros().toPlainString()
}
