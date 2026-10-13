package one.mixin.android.ui.home.web3.trade.perps

import java.math.BigDecimal
import java.math.RoundingMode

private const val TP_SL_CALCULATION_SCALE = 16

internal data class TpSlCalculationBasis(
    val entryPrice: BigDecimal,
    val margin: BigDecimal,
    val absoluteQuantity: BigDecimal,
) {
    fun pnlAt(targetPrice: BigDecimal, isLong: Boolean): TpSlPnlResult? {
        if (targetPrice <= BigDecimal.ZERO) return null
        val direction = if (isLong) BigDecimal.ONE else BigDecimal.ONE.negate()
        val signedPnl = targetPrice.subtract(entryPrice).multiply(absoluteQuantity).multiply(direction)
        if (signedPnl.compareTo(BigDecimal.ZERO) == 0) return null
        val signedRoiPercent = signedPnl
            .multiply(BigDecimal(100))
            .divide(margin, TP_SL_CALCULATION_SCALE, RoundingMode.HALF_UP)
        return TpSlPnlResult(signedPnl = signedPnl, signedRoiPercent = signedRoiPercent)
    }

    fun zeroPriceRoiPercentCeiling(): BigDecimal = entryPrice
        .multiply(absoluteQuantity)
        .multiply(BigDecimal(100))
        .divide(margin, TP_SL_CALCULATION_SCALE, RoundingMode.HALF_UP)
}

internal data class TpSlPnlResult(
    val signedPnl: BigDecimal,
    val signedRoiPercent: BigDecimal,
)

internal fun resolveTpSlConversionBasis(
    entryPrice: String,
    currentPrice: String,
    marginAmount: String,
    positionQuantity: String?,
    leverage: Int,
): TpSlCalculationBasis? = resolveTpSlCalculationBasis(
    entryPrice = entryPrice,
    currentPrice = currentPrice,
    marginAmount = if (positionQuantity == null &&
        (marginAmount.isBlank() || marginAmount.toBigDecimalOrNull()?.signum() == 0)
    ) "1" else marginAmount,
    positionQuantity = positionQuantity,
    leverage = leverage,
)

internal fun resolveTpSlCalculationBasis(
    entryPrice: String,
    currentPrice: String,
    marginAmount: String,
    positionQuantity: String?,
    leverage: Int,
): TpSlCalculationBasis? {
    val margin = marginAmount.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO } ?: return null
    val parsedEntry = entryPrice.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO }
    val entry = if (positionQuantity == null) {
        parsedEntry ?: currentPrice.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO } ?: return null
    } else {
        parsedEntry ?: return null
    }
    val absoluteQuantity = if (positionQuantity == null) {
        if (leverage <= 0) return null
        margin.multiply(BigDecimal(leverage))
            .divide(entry, TP_SL_CALCULATION_SCALE, RoundingMode.HALF_UP)
            .takeIf { it > BigDecimal.ZERO }
            ?: return null
    } else {
        positionQuantity.toBigDecimalOrNull()?.abs()?.takeIf { it > BigDecimal.ZERO } ?: return null
    }
    return TpSlCalculationBasis(entry, margin, absoluteQuantity)
}

internal fun targetPriceForRoiPercent(
    basis: TpSlCalculationBasis,
    percentMagnitudeInput: String,
    isLong: Boolean,
    isTakeProfit: Boolean,
    priceScale: Int,
): String {
    val magnitude = percentMagnitudeInput.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO } ?: return ""
    val signedRoi = magnitude
        .divide(BigDecimal(100), TP_SL_CALCULATION_SCALE, RoundingMode.HALF_UP)
        .let { if (isTakeProfit) it else it.negate() }
    val direction = if (isLong) BigDecimal.ONE else BigDecimal.ONE.negate()
    val targetPrice = basis.entryPrice.add(
        signedRoi.multiply(basis.margin)
            .divide(basis.absoluteQuantity, TP_SL_CALCULATION_SCALE, RoundingMode.HALF_UP)
            .multiply(direction)
    )
    if (targetPrice <= BigDecimal.ZERO) return ""
    return formatPerpsPriceInput(targetPrice, priceScale)
}

internal fun resolveTpSlLiquidationBound(
    serverLiquidationPrice: String?,
    entryPrice: String,
    currentPrice: String,
    leverage: Int,
    isLong: Boolean,
): BigDecimal? {
    if (!serverLiquidationPrice.isNullOrBlank()) {
        return serverLiquidationPrice.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO }
    }
    if (leverage <= 0) return null
    val base = entryPrice.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO }
        ?: currentPrice.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO }
        ?: return null
    val offset = BigDecimal.ONE.divide(BigDecimal(leverage), TP_SL_CALCULATION_SCALE, RoundingMode.HALF_UP)
    return base.multiply(if (isLong) BigDecimal.ONE.subtract(offset) else BigDecimal.ONE.add(offset))
        .takeIf { it > BigDecimal.ZERO }
}

internal fun isStopBeyondLiquidation(
    stopPrice: BigDecimal,
    liquidationBound: BigDecimal?,
    isLong: Boolean,
): Boolean = when {
    liquidationBound == null -> true
    isLong -> stopPrice > liquidationBound
    else -> stopPrice < liquidationBound
}
