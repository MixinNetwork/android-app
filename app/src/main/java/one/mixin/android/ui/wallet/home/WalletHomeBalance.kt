package one.mixin.android.ui.wallet.home

import one.mixin.android.api.response.perps.PerpsPositionItem
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

internal fun calculateWalletHomeTotalFiat(
    tokenFiat: BigDecimal,
    positionUsd: BigDecimal,
    fiatRate: BigDecimal,
    cashUsd: BigDecimal = BigDecimal.ZERO,
    wealthUsd: BigDecimal = BigDecimal.ZERO,
): BigDecimal = tokenFiat + positionUsd.add(cashUsd).add(wealthUsd).multiply(fiatRate)

internal fun calculateWalletHomeTokenFiat(
    totalUsd: BigDecimal,
    fiatRate: BigDecimal,
): BigDecimal = totalUsd.multiply(fiatRate)

internal fun calculateWalletHomeBtcTotal(
    tokenFiat: BigDecimal,
    tokenBtc: BigDecimal,
    bitcoinPriceUsd: BigDecimal?,
    fiatRate: BigDecimal,
): BigDecimal {
    val validBitcoinPriceUsd = bitcoinPriceUsd?.takeIf { it > BigDecimal.ZERO } ?: return tokenBtc
    val validFiatRate = fiatRate.takeIf { it > BigDecimal.ZERO } ?: BigDecimal.ONE
    return tokenFiat
        .divide(validFiatRate, 16, RoundingMode.HALF_UP)
        .divide(validBitcoinPriceUsd, 16, RoundingMode.HALF_UP)
}

internal fun calculateWalletHomePositionMarginUsd(
    margins: Iterable<String?>,
): BigDecimal =
    margins.fold(BigDecimal.ZERO) { total, margin ->
        total + (margin?.toBigDecimalOrNull() ?: BigDecimal.ZERO)
    }

internal fun List<PerpsPositionItem>.positionMarginUsdTotal(): BigDecimal =
    calculateWalletHomePositionMarginUsd(
        margins = map { it.margin },
    )

internal fun usdBalanceAmountText(amount: BigDecimal): String =
    usdDecimalFormat(maxFractionDigits = 2).format(amount)

internal fun usdCurrencyAmountText(amount: BigDecimal): String =
    "$${usdDecimalFormat(maxFractionDigits = 8).format(amount)}"

internal fun tokenAmountText(amount: BigDecimal): String =
    usdDecimalFormat(maxFractionDigits = 8).format(amount)

private fun usdDecimalFormat(maxFractionDigits: Int): DecimalFormat =
    DecimalFormat(",##0.${"#".repeat(maxFractionDigits)}").apply {
        roundingMode = RoundingMode.DOWN
        this.maximumFractionDigits = maxFractionDigits
        minimumFractionDigits = 0
    }
