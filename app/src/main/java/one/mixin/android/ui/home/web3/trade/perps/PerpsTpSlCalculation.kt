package one.mixin.android.ui.home.web3.trade.perps

import java.math.BigDecimal
import java.math.RoundingMode

private const val TP_SL_CALCULATION_SCALE = 16

internal data class TpSlCalculationBasis(
    val entryPrice: BigDecimal,
    val margin: BigDecimal,
    val absoluteQuantity: BigDecimal,
    val feeRate: BigDecimal,
) {
    fun pnlAt(targetPrice: BigDecimal, isLong: Boolean): TpSlPnlResult? {
        if (targetPrice <= BigDecimal.ZERO) return null
        val direction = if (isLong) BigDecimal.ONE else BigDecimal.ONE.negate()
        val pricePnl = targetPrice.subtract(entryPrice).multiply(absoluteQuantity).multiply(direction)
        // Realized PnL includes the open fee and the close fee at the trigger price.
        val signedPnl = pricePnl
            .subtract(entryPrice.multiply(absoluteQuantity).multiply(feeRate))
            .subtract(targetPrice.multiply(absoluteQuantity).multiply(feeRate))
        val signedRoiPercent = signedPnl
            .multiply(BigDecimal(100))
            .divide(margin, TP_SL_CALCULATION_SCALE, RoundingMode.HALF_UP)
        return TpSlPnlResult(signedPnl = signedPnl, signedRoiPercent = signedRoiPercent)
    }

    fun priceAtPnl(netPnl: BigDecimal, isLong: Boolean, priceScale: Int): BigDecimal? {
        val denominator = absoluteQuantity.multiply(
            if (isLong) BigDecimal.ONE.subtract(feeRate) else BigDecimal.ONE.add(feeRate)
        )
        if (denominator.signum() <= 0) return null
        val numerator = if (isLong) {
            netPnl.add(absoluteQuantity.multiply(entryPrice).multiply(BigDecimal.ONE.add(feeRate)))
        } else {
            absoluteQuantity.multiply(entryPrice).multiply(BigDecimal.ONE.subtract(feeRate)).subtract(netPnl)
        }
        return numerator.divide(denominator, priceScale, if (isLong) RoundingMode.CEILING else RoundingMode.FLOOR)
    }

    fun zeroPriceRoiPercentCeiling(isLong: Boolean): BigDecimal {
        val openFee = entryPrice.multiply(absoluteQuantity).multiply(feeRate)
        val pricePnlAtZero = entryPrice.multiply(absoluteQuantity)
        val netMagnitude = if (isLong) pricePnlAtZero.add(openFee) else pricePnlAtZero.subtract(openFee)
        return netMagnitude
            .multiply(BigDecimal(100))
            .divide(margin, TP_SL_CALCULATION_SCALE, RoundingMode.HALF_UP)
    }
}

internal data class TpSlPnlResult(
    val signedPnl: BigDecimal,
    val signedRoiPercent: BigDecimal,
)

internal fun resolveTpSlCalculationBasis(
    entryPrice: String,
    currentPrice: String,
    marginAmount: String,
    positionQuantity: String?,
    leverage: Int,
    feeRate: String? = null,
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
    val parsedFeeRate = feeRate?.toBigDecimalOrNull()?.takeIf { it >= BigDecimal.ZERO && it < BigDecimal.ONE } ?: return null
    return TpSlCalculationBasis(entry, margin, absoluteQuantity, parsedFeeRate)
}

internal fun targetPriceForRoiPercent(
    basis: TpSlCalculationBasis,
    percentMagnitudeInput: String,
    isLong: Boolean,
    isProfit: Boolean,
    priceScale: Int,
): String {
    val magnitude = percentMagnitudeInput.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO } ?: return ""
    val signedRoi = magnitude.movePointLeft(2).let { if (isProfit) it else it.negate() }
    val netPnl = signedRoi.multiply(basis.margin)
    val safeScale = priceScale.coerceAtLeast(0)
    val roundedPrice = basis.priceAtPnl(netPnl, isLong, safeScale) ?: return ""
    if (roundedPrice <= BigDecimal.ZERO) return ""
    return formatPerpsPriceInput(roundedPrice, safeScale)
}

internal fun resolveTpSlLiquidationBound(serverLiquidationPrice: String?): BigDecimal? =
    serverLiquidationPrice?.toBigDecimalOrNull()?.takeIf { it >= BigDecimal.ZERO }
